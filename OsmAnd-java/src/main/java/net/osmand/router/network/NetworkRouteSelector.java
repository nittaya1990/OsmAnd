package net.osmand.router.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadRect;
import net.osmand.router.network.NetworkRouteContext.NetworkRouteSegment;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class NetworkRouteSelector {
	
	private static final String ROUTE_KEY_VALUE_SEPARATOR = "__";

	private static final boolean GROW_ALGORITHM = false;
	private static final int MAX_ITERATIONS = 16000;
	// works only if road in same tile
	private static final double MAX_RADIUS_HOLE = 30;
	private static final double CONNECT_POINTS_DISTANCE = 100;

	
	private final NetworkRouteContext rCtx;
	
	// TODO 1. Search by bbox
	// TODO 2. FIX & implement work with routing tags
	// TEST:
	// 1. Round routes
	// 2. Loop & middle & roundabout: https://www.openstreetmap.org/way/23246638#map=19/47.98180/11.28338 [5]
	//    Roundabout: Lots deviations https://www.openstreetmap.org/relation/1075081#map=8/47.656/10.456 [44] -> 29
	// 3. https://www.openstreetmap.org/relation/1200009#map=8/60.592/10.940 [25]
	// 4. https://www.openstreetmap.org/relation/138401#map=19/51.06795/7.37955 [6] -> 1
	// 5. https://www.openstreetmap.org/relation/145490#map=16/51.0607/7.3596 [2] -> 1
	
	public NetworkRouteSelector(BinaryMapIndexReader[] files, NetworkRouteSelectorFilter filter) {
		this(files, filter, false);
	}
	
	public NetworkRouteSelector(BinaryMapIndexReader[] files, NetworkRouteSelectorFilter filter, boolean routing) {
		if (filter == null) {
			filter = new NetworkRouteSelectorFilter();
		}
		rCtx = new NetworkRouteContext(files, filter, routing);
	}
	
	public NetworkRouteContext getNetworkRouteContext() {
		return rCtx;
	}
	
	public Map<RouteKey, GPXFile> getRoutes(RenderedObject renderedObject) throws IOException {
		int x = renderedObject.getX().get(0);
		int y = renderedObject.getY().get(0);
		return getRoutes(x, y);
	}

	public Map<RouteKey, GPXFile> getRoutes(int x, int y) throws IOException {
		Map<RouteKey, GPXFile> res = new LinkedHashMap<RouteKey, GPXUtilities.GPXFile>();
		for (NetworkRouteSegment segment : rCtx.loadRouteSegment(x, y)) {
			if (res.containsKey(segment.routeKey)) {
				continue;
			}
			if (GROW_ALGORITHM) {
				growAlgorithm(segment, res);
			} else {
				connectAlgorithm(segment, res);
			}
		}
		return res;
	}
	
	public Map<RouteKey, GPXFile> getRoutes(QuadRect bBox) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	
	private static class NetworkRouteSegmentChain {
		NetworkRouteSegment start;
		List<NetworkRouteSegment> connected;
		
		public int getSize() {
			return 1 + (connected == null ? 0 : connected.size());
		}
		
		public NetworkRouteSegment getLast() {
			if (connected != null && connected.size() > 0) {
				return connected.get(connected.size() - 1);
			}
			return start;
		}
		
		public int getEndPointX() {
			return getLast().getEndPointX();
		}
		
		public int getEndPointY() {
			return getLast().getEndPointY();
		}
		
		public void addChain(NetworkRouteSegmentChain toAdd) {
			if (connected == null) {
				connected = new ArrayList<>();
			}
			connected.add(toAdd.start);
			if (toAdd.connected != null) {
				connected.addAll(toAdd.connected);
			}
		}

		public void setStart(NetworkRouteSegment newStart) {
			start = newStart;
		}
		
		public void setEnd(NetworkRouteSegment newEnd) {
			if (connected != null) {
				connected.remove(connected.size() - 1);
				connected.add(newEnd);
			} else {
				start = newEnd;
			}
		}
	}
	
	private List<NetworkRouteSegmentChain> getByPoint(Map<Long, List<NetworkRouteSegmentChain>> chains, long pnt,
			int radius, NetworkRouteSegmentChain exclude) {
		List<NetworkRouteSegmentChain> list = null;
		if (radius == 0) {
			list = chains.get(pnt);
			if (list != null) {
				if (!list.contains(exclude)) {
					return list;
				} else if (list.size() == 1) {
					list = null;
				} else {
					list = new ArrayList<>(list);
					list.remove(exclude);
				}
			}
		} else {
			int x = NetworkRouteContext.getXFromLong(pnt);
			int y = NetworkRouteContext.getYFromLong(pnt);
			Iterator<Entry<Long, List<NetworkRouteSegmentChain>>> it = chains.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Long, List<NetworkRouteSegmentChain>> e = it.next();
				int x2 = NetworkRouteContext.getXFromLong(e.getKey());
				int y2 = NetworkRouteContext.getYFromLong(e.getKey());
				if (MapUtils.squareRootDist31(x, y, x2, y2) < radius) {
					if (list == null) {
						list = new ArrayList<>();
					}
					for (NetworkRouteSegmentChain c : e.getValue()) {
						if (c != exclude) {
							list.add(c);
						}
					}
				}
			}
		}
		if (list == null) {
			return Collections.emptyList();
		}
		return list;
	}
	
	private void connectAlgorithm(NetworkRouteSegment segment, Map<RouteKey, GPXFile> res) throws IOException {
		RouteKey rkey = segment.routeKey;
		List<NetworkRouteSegment> loaded = new ArrayList<>();
		debug("START ", null, segment);
		loadData(segment, rkey, loaded);
		System.out.println("About to merge: " + loaded.size());
		Map<Long, List<NetworkRouteSegmentChain>> chains = createChainStructure(loaded);
		Map<Long, List<NetworkRouteSegmentChain>> endChains = prepareEndChain(chains);
		// Merged
		connectSimpleMerge(chains, endChains, 0);
		connectSimpleMerge(chains, endChains, (int) CONNECT_POINTS_DISTANCE / 2);
		connectSimpleMerge(chains, endChains, (int) CONNECT_POINTS_DISTANCE);
		connectSimpleMerge(chains, endChains, (int) CONNECT_POINTS_DISTANCE * 2);
		connectToLongestChain(chains, endChains, 0);
		connectSimpleMerge(chains, endChains, (int) CONNECT_POINTS_DISTANCE);
		
		List<NetworkRouteSegment> lst = flattenChainStructure(chains);
		GPXFile fl = createGpxFile(lst);
		res.put(segment.routeKey, fl);
		System.out.println("Segments size: " + fl.tracks.get(0).segments.size());
		debug("FINISH " + lst.size(), null, segment);
	}


	private int connectToLongestChain(Map<Long, List<NetworkRouteSegmentChain>> chains,
			Map<Long, List<NetworkRouteSegmentChain>> endChains, int rad) {
		int merged = 0;
		int rdC = 2 * rad + 5;
		boolean changed = true;
		while (changed) {
			changed = false;
			mainLoop: for (List<NetworkRouteSegmentChain> lst : chains.values()) {
				for (NetworkRouteSegmentChain it : lst) {
					long pnt = NetworkRouteContext.convertPointToLong(it.start.getStartPointX(),
							it.start.getStartPointY());
					Set<NetworkRouteSegmentChain> set = new HashSet<>();
					set.add(it);
					set.addAll(getByPoint(chains, pnt, rad, it));
					set.addAll(getByPoint(endChains, pnt, rad, it));
					List<NetworkRouteSegmentChain> l = new ArrayList<>(set);
					Collections.sort(l, new Comparator<NetworkRouteSegmentChain>() {
						@Override
						public int compare(NetworkRouteSegmentChain o1, NetworkRouteSegmentChain o2) {
							return -Integer.compare(o1.getSize(), o2.getSize());
						}
					});
					if (l.size() > 2) {
						// take 2 longest
						NetworkRouteSegmentChain first = l.get(0);
						NetworkRouteSegmentChain second = l.get(1);
						if (MapUtils.squareRootDist31(first.getEndPointX(), first.getEndPointY(), second.getEndPointX(),
								second.getEndPointY()) < rdC) {
							NetworkRouteSegmentChain secondReversed = chainReverse(chains, endChains, second);
							chainAdd(chains, endChains, first, secondReversed);
						} else if (MapUtils.squareRootDist31(first.start.getStartPointX(), first.start.getStartPointY(),
								second.start.getStartPointX(), second.start.getStartPointY()) < rdC) {
							NetworkRouteSegmentChain secondReversed = chainReverse(chains, endChains, second);
							chainAdd(chains, endChains, first, secondReversed);
						} else if (MapUtils.squareRootDist31(first.getEndPointX(), first.getEndPointY(),
								second.start.getStartPointX(), second.start.getStartPointY()) < rdC) {
							chainAdd(chains, endChains, first, second);
						} else if (MapUtils.squareRootDist31(second.getEndPointX(), second.getEndPointY(),
								first.start.getStartPointX(), first.start.getStartPointY()) < rdC) {
							chainAdd(chains, endChains, second, first);
						} else {
							throw new IllegalStateException();
						}
						merged++;
						changed = true;
						break mainLoop;

					}
				}
			}
		}
		System.out.println(String.format("Connect longest alternative chains: %d (radius %d)", merged, rad));
		return merged;
	}

	private int connectSimpleMerge(Map<Long, List<NetworkRouteSegmentChain>> chains,
			Map<Long, List<NetworkRouteSegmentChain>> endChains, int rad) {
		int merged = 1;
		while (merged > 0) {
			int rs = reverseToConnectMore(chains, endChains, rad);
			merged = connectSimpleStraight(chains, endChains, rad);
			System.out.println(String.format("Simple merged: %d, reversed: %d (radius %d)", merged, rs, rad));
		}
		return merged;
	}
	
	
	private int reverseToConnectMore(Map<Long, List<NetworkRouteSegmentChain>> chains, Map<Long, List<NetworkRouteSegmentChain>> endChains, int rad) {
		int reversed = 0;
		List<Long> longPoints = new ArrayList<>(chains.keySet());
		for (Long startPnt : longPoints) {
			List<NetworkRouteSegmentChain> vls = chains.get(startPnt);
			for (int i = 0; vls != null && i < vls.size(); i++) {
				NetworkRouteSegmentChain it = vls.get(i);
				long pnt = NetworkRouteContext.convertPointToLong(it.getEndPointX(), it.getEndPointY());
				// 1. reverse if 2 segments start from same point
				boolean noStartFromEnd = getByPoint(chains, pnt, rad, null).size() == 0;
				boolean reverse = (noStartFromEnd && i == 1);
				// 2. reverse 2 segments ends at same point
				reverse |= i == 0 && getByPoint(endChains, pnt, rad, null).size() > 1 && noStartFromEnd;
				if (reverse) {
					chainReverse(chains, endChains, it);
					reversed++;
					break;
				}
			}
		}
		return reversed;
	}

	private int connectSimpleStraight(Map<Long, List<NetworkRouteSegmentChain>> chains, 
			Map<Long, List<NetworkRouteSegmentChain>> endChains, int rad) {
		int merged = 0;
		boolean changed = true;
		while (changed) {
			changed = false;
			mainLoop: for (List<NetworkRouteSegmentChain> lst : chains.values()) {
				for (NetworkRouteSegmentChain it : lst) {
					long pnt = NetworkRouteContext.convertPointToLong(it.getEndPointX(), it.getEndPointY());
					List<NetworkRouteSegmentChain> connectNextLst = getByPoint(chains, pnt, rad, it);
					List<NetworkRouteSegmentChain> connectToEndLst = getByPoint(endChains, pnt, rad, it); // equal to c
					// no alternative join
					if (connectNextLst.size() == 1 && connectToEndLst.size() == 0) {
						chainAdd(chains, endChains, it, connectNextLst.get(0));
						changed = true;
						merged++;
						break mainLoop;
					}
				}
			}
		}
		return merged;
	}
	
	private NetworkRouteSegmentChain chainReverse(Map<Long, List<NetworkRouteSegmentChain>> chains,
			Map<Long, List<NetworkRouteSegmentChain>> endChains, NetworkRouteSegmentChain it) {
		long startPnt = NetworkRouteContext.convertPointToLong(it.start.getStartPointX(), it.start.getStartPointY());
		long pnt = NetworkRouteContext.convertPointToLong(it.getEndPointX(), it.getEndPointY());
		List<NetworkRouteSegment> lst = new ArrayList<>();
		lst.add(0, it.start.inverse());
		if (it.connected != null) {
			for (NetworkRouteSegment s : it.connected) {
				lst.add(0, s.inverse());
			}
		}
		remove(chains, startPnt, it);
		remove(endChains, pnt, it);
		NetworkRouteSegmentChain newChain = new NetworkRouteSegmentChain();
		newChain.start = lst.remove(0);
		newChain.connected = lst;
		add(chains, NetworkRouteContext.convertPointToLong(newChain.start.getStartPointX(),
				newChain.start.getStartPointY()), newChain);
		add(endChains, NetworkRouteContext.convertPointToLong(newChain.getEndPointX(), newChain.getEndPointY()),
				newChain);
		return newChain;
	}

	private void chainAdd(Map<Long, List<NetworkRouteSegmentChain>> chains,
			Map<Long, List<NetworkRouteSegmentChain>> endChains, NetworkRouteSegmentChain it, NetworkRouteSegmentChain toAdd) {
		if (it == toAdd) {
			throw new IllegalStateException();
		}
		remove(chains, NetworkRouteContext.convertPointToLong(toAdd.start.getStartPointX(), toAdd.start.getStartPointY()), toAdd);
		remove(endChains, NetworkRouteContext.convertPointToLong(toAdd.getEndPointX(), toAdd.getEndPointY()), toAdd);
		remove(endChains, NetworkRouteContext.convertPointToLong(it.getEndPointX(), it.getEndPointY()), it);
		double minStartDist = MapUtils.squareRootDist31(it.getEndPointX(), it.getEndPointY(), toAdd.start.getStartPointX(),toAdd.start.getStartPointY());
		double minLastDist = minStartDist;
		int minStartInd = toAdd.start.start;
		for (int i = 0; i < toAdd.start.getPointsLength(); i++) {
			double m = MapUtils.squareRootDist31(it.getEndPointX(), it.getEndPointY(), toAdd.start.getPoint31XTile(i),
					toAdd.start.getPoint31YTile(i));
			if (m < minStartDist && minStartInd != i) {
				minStartInd = i;
				minStartDist = m;
			}
		}
		NetworkRouteSegment lastIt = it.getLast();
		int minLastInd = lastIt.end;
		for (int i = 0; i < lastIt.getPointsLength(); i++) {
			double m = MapUtils.squareRootDist31(lastIt.getPoint31XTile(i), lastIt.getPoint31YTile(i),
					toAdd.start.getStartPointX(), toAdd.start.getStartPointY());
			if (m < minLastDist && minLastInd != i) {
				minLastInd = i;
				minLastDist = m;
			}
		}
		if (minLastDist > minStartDist) {
			if (minStartInd != toAdd.start.start) {
				toAdd.setStart(new NetworkRouteSegment(toAdd.start, minStartInd, toAdd.start.end));
			}
		} else {
			if (minLastInd != lastIt.end) {
				it.setEnd(new NetworkRouteSegment(lastIt, lastIt.start, minLastInd));
			}
		}
		
		it.addChain(toAdd);
		add(endChains, NetworkRouteContext.convertPointToLong(it.getEndPointX(), it.getEndPointY()), it);
	}

	private void add(Map<Long, List<NetworkRouteSegmentChain>> chains, long pnt, NetworkRouteSegmentChain chain) {
		List<NetworkRouteSegmentChain> lst = chains.get(pnt);
		if (lst == null) {
			lst = new ArrayList<>();
			chains.put(pnt, lst);
		}
		lst.add(chain);
	}
	
	private void remove(Map<Long, List<NetworkRouteSegmentChain>> chains, long pnt, NetworkRouteSegmentChain toRemove) {
		List<NetworkRouteSegmentChain> lch = chains.get(pnt);
		if (lch == null) {
			throw new IllegalStateException();
		} else {
			if (!lch.remove(toRemove)) {
				throw new IllegalStateException();
			}
			if (lch.isEmpty()) {
				chains.remove(pnt);
			}
		}
	}

	private List<NetworkRouteSegment> flattenChainStructure(Map<Long, List<NetworkRouteSegmentChain>> chains) {
		List<NetworkRouteSegment> lst = new ArrayList<>();
//		int i = 0;
		List<NetworkRouteSegmentChain> chainsFlat = new ArrayList<NetworkRouteSegmentChain>();
		for (List<NetworkRouteSegmentChain> ch : chains.values()) {
			chainsFlat.addAll(ch);
		}
		Collections.sort(chainsFlat, new Comparator<NetworkRouteSegmentChain>() {
			@Override
			public int compare(NetworkRouteSegmentChain o1, NetworkRouteSegmentChain o2) {
				return -Integer.compare(o1.getSize(), o2.getSize());
			}
		});
		for (NetworkRouteSegmentChain c : chainsFlat) {
//			if (i++ > 5) {
//				break;
//			}
			lst.add(c.start);
			if (c.connected != null) {
				lst.addAll(c.connected);
			}
		}
		return lst;
	}

	private Map<Long, List<NetworkRouteSegmentChain>> prepareEndChain(Map<Long, List<NetworkRouteSegmentChain>> chains) {
		Map<Long, List<NetworkRouteSegmentChain>> endChains = new LinkedHashMap<>();
		for (List<NetworkRouteSegmentChain> ch : chains.values()) {
			for (NetworkRouteSegmentChain chain : ch) {
				add(endChains, NetworkRouteContext.convertPointToLong(chain.getEndPointX(), chain.getEndPointY()), chain);
			}
		}
		return endChains;
	}

	
	private Map<Long, List<NetworkRouteSegmentChain>> createChainStructure(List<NetworkRouteSegment> lst) {
		Map<Long, List<NetworkRouteSegmentChain>> chains = new LinkedHashMap<>();
		for (NetworkRouteSegment s : lst) {
			NetworkRouteSegmentChain chain = new NetworkRouteSegmentChain();
			chain.start = s;
			long pnt = NetworkRouteContext.convertPointToLong(s.getStartPointX(), s.getStartPointY());
			add(chains, pnt, chain);
		}
		return chains;
	}

	private void loadData(NetworkRouteSegment segment, RouteKey rkey, List<NetworkRouteSegment> lst)
			throws IOException {
		TLongArrayList queue = new TLongArrayList();
		Set<Long> visitedTiles = new HashSet<>();
		Set<Long> objIds = new HashSet<>();
		long start = NetworkRouteContext.getTileId(segment.getStartPointX(), segment.getStartPointY());
		long end = NetworkRouteContext.getTileId(segment.getEndPointX(), segment.getEndPointY());
		queue.add(start);
		queue.add(end);
		while (!queue.isEmpty()) {
			long tile = queue.get(queue.size() - 1);
			queue.remove(queue.size() - 1, 1);
			if (!visitedTiles.add(tile)) {
				continue;
			}
			int left = NetworkRouteContext.getX31FromTileId(tile, 0);
			int top = NetworkRouteContext.getY31FromTileId(tile, 0);
			int right = NetworkRouteContext.getX31FromTileId(tile, 1);
			int bottom = NetworkRouteContext.getY31FromTileId(tile, 1);
			Map<RouteKey, List<NetworkRouteSegment>> tiles = rCtx.loadRouteSegmentTile(left, top, right - 1, bottom - 1, rkey);
			List<NetworkRouteSegment> loaded = tiles.get(rkey);
			int sz = loaded == null ? 0 : loaded.size();
			System.out.println(String.format("Load tile %d: %d segments", tile, sz));
			if (sz == 0) {
				continue;
			}
			for (NetworkRouteSegment s : loaded) {
				if (objIds.add(s.getId())) {
					lst.add(s);
				}
			}
			queue.add(NetworkRouteContext.getTileId(right, bottom));
			queue.add(NetworkRouteContext.getTileId(right, top));
			queue.add(NetworkRouteContext.getTileId(right, top - 1));
			queue.add(NetworkRouteContext.getTileId(left - 1, bottom));
			queue.add(NetworkRouteContext.getTileId(left - 1, top));
			queue.add(NetworkRouteContext.getTileId(left - 1, top - 1));
			queue.add(NetworkRouteContext.getTileId(left, bottom));
			// queue.add(NetworkRouteContext.getTileId(left, top)); // same
			queue.add(NetworkRouteContext.getTileId(left, top - 1));
		}
	}
	
	
	private void growAlgorithm(NetworkRouteSegment segment, Map<RouteKey, GPXFile> res) throws IOException {
		List<NetworkRouteSegment> lst = new ArrayList<>();
		TLongHashSet visitedIds = new TLongHashSet();
		visitedIds.add(segment.getId());
		lst.add(segment.inverse());
		debug("START ", null, segment);
		int it = 0;
		while (it++ < MAX_ITERATIONS) {
			if (!grow(lst, visitedIds, true, false)) {
				if (!grow(lst, visitedIds, true, true)) {
					it = 0;
					break;
				}
			}
		}
		Collections.reverse(lst);
		for (int i = 0; i < lst.size(); i++) {
			lst.set(i, lst.get(i).inverse());
		}
		while (it++ < MAX_ITERATIONS) {
			if (!grow(lst, visitedIds, false, false)) {
				if (!grow(lst, visitedIds, false, true)) {
					it = 0;
					break;
				}
			}
		}
		if (it != 0) {
			RouteKey rkey = segment.routeKey;
			TIntArrayList ids = new TIntArrayList();
			for (int i = lst.size() - 1; i > 0 && i > lst.size() - 50; i--) {
				ids.add((int) (lst.get(i).getId() >> 7));
			}
			String msg = "Route likely has a loop: " + rkey + " iterations " + it + " ids " + ids;
			System.err.println(msg); // throw new IllegalStateException();
		}
		res.put(segment.routeKey, createGpxFile(lst));
		debug("FINISH " + lst.size(), null, segment);

	}
	
	private void debug(String msg, Boolean reverse, NetworkRouteSegment ld) {
		System.out.println(msg + (reverse == null ? "" : (reverse ? '-' : '+')) + " " + ld);
	}
	
	private boolean grow(List<NetworkRouteSegment> lst, TLongHashSet visitedIds, boolean reverse, boolean approximate) throws IOException {
		int lastInd = lst.size() - 1;
		NetworkRouteSegment obj = lst.get(lastInd);
		List<NetworkRouteSegment> objs = approximate ? rCtx.loadNearRouteSegment(obj.getEndPointX(), obj.getEndPointY(), MAX_RADIUS_HOLE) : 
			rCtx.loadRouteSegment(obj.getEndPointX(), obj.getEndPointY());
		for (NetworkRouteSegment ld : objs) {
			debug("  CHECK", reverse, ld);
			if (ld.routeKey.equals(obj.routeKey) && !visitedIds.contains(ld.getId()) ) {
				// && ld.getId() != obj.getId() && otherSide.getId() != ld.getId()) {
				// visitedIds.add((ld.getId() << 14) + (reverse ? ld.end : ld.start))
				if (visitedIds.add(ld.getId())) { // forbid same segment twice
					debug(">ACCEPT", reverse, ld);
					lst.add(ld);
					return true;
				} else {
					// loop
					return false;
				}
			}
		}
		return false;
	}

	

	private GPXFile createGpxFile(List<NetworkRouteSegment> segmentList) {
		GPXFile gpxFile = new GPXFile(null, null, null);
		GPXUtilities.Track track = new GPXUtilities.Track();
		GPXUtilities.TrkSegment trkSegment = new GPXUtilities.TrkSegment();
		for (NetworkRouteSegment segment : segmentList) {
			int inc = segment.start < segment.end ? 1 : -1;
			for (int i = segment.start;; i += inc) {
				GPXUtilities.WptPt point = new GPXUtilities.WptPt();
				point.lat = MapUtils.get31LatitudeY(segment.getPoint31YTile(i));
				point.lon = MapUtils.get31LongitudeX(segment.getPoint31XTile(i));
				if (i == segment.start && trkSegment.points.size() > 0) {
					WptPt lst = trkSegment.points.get(trkSegment.points.size() - 1);
					double dst = MapUtils.getDistance(lst.lat, lst.lon, point.lat, point.lon);
					if (dst > 1) {
						if (dst > CONNECT_POINTS_DISTANCE) {
							track.segments.add(trkSegment);
							trkSegment = new GPXUtilities.TrkSegment();
						}
						trkSegment.points.add(point);
					}
				} else {
					trkSegment.points.add(point);
				}
				if (i == segment.end) {
					break;
				}
			}
		}
		track.segments.add(trkSegment);
		gpxFile.tracks.add(track);
		return gpxFile;
	}


	public static class NetworkRouteSelectorFilter {
		public Set<RouteKey> keyFilter = null; // null - all
		public Set<RouteType> typeFilter = null; // null -  all
		
		public List<RouteKey> convert(BinaryMapDataObject obj) {
			return filterKeys(RouteType.getRouteKeys(obj));
		}

		public List<RouteKey> convert(RouteDataObject obj) {
			return filterKeys(RouteType.getRouteKeys(obj));
		}


		private List<RouteKey> filterKeys(List<RouteKey> keys) {
			if (keyFilter == null && typeFilter == null) {
				return keys;
			}
			Iterator<RouteKey> it = keys.iterator();
			while (it.hasNext()) {
				RouteKey key = it.next();
				if (keyFilter != null && !keyFilter.contains(key)) {
					it.remove();
				} else if (typeFilter != null && !typeFilter.contains(key.type)) {
					it.remove();
				}
			}
			return keys;
		}

	}
	
	public static class RouteKey {
		
		public final RouteType type;
		public final Set<String> set = new TreeSet<String>();
		
		public RouteKey(RouteType routeType) {
			this.type = routeType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((set == null) ? 0 : set.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RouteKey other = (RouteKey) obj;
			if (set == null) {
				if (other.set != null)
					return false;
			} else if (!set.equals(other.set))
				return false;
			if (type != other.type)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Route [type=" + type + ", set=" + set + "]";
		}
		
	}
	
	public enum RouteType {

		HIKING("hiking"),
		BICYCLE("bicycle"),
		MTB("mtb"),
		HORSE("horse");

		private final String tag;
		private final String tagPrefix;

		RouteType(String tag) {
			this.tag = tag;
			this.tagPrefix = "route_" + tag + "_";
		}

		public String getTag() {
			return tag;
		}

		public static List<RouteKey> getRouteKeys(RouteDataObject obj) {
			Map<String, String> tags = new TreeMap<>();
			for (int i = 0; obj.nameIds != null && i < obj.nameIds.length; i++) {
				int nameId = obj.nameIds[i];
				String value = obj.names.get(nameId);
				RouteTypeRule rt = obj.region.quickGetEncodingRule(nameId);
				if (rt != null) {
					tags.put(rt.getTag(), value);
				}
			}
			for (int i = 0; obj.types != null && i < obj.types.length; i++) {
				RouteTypeRule rt = obj.region.quickGetEncodingRule(obj.types[i]);
				if (rt != null) {
					tags.put(rt.getTag(), rt.getValue());
				}
			}
			return getRouteKeys(tags);
		}


		public static List<RouteKey> getRouteStringKeys(RenderedObject o) {
			Map<String, String> tags = o.getTags();
			return getRouteKeys(tags);
		}
		
		public static List<RouteKey> getRouteKeys(BinaryMapDataObject bMdo) {
			Map<String, String> tags = new TreeMap<>();
			for (int i = 0; i < bMdo.getObjectNames().keys().length; i++) {
				int keyInd = bMdo.getObjectNames().keys()[i];
				TagValuePair tp = bMdo.getMapIndex().decodeType(keyInd);
				String value = bMdo.getObjectNames().get(keyInd);
				if (tp != null) {
					tags.put(tp.tag, value);
				}
			}
			int[] tps = bMdo.getAdditionalTypes();
			for (int i = 0; i < tps.length; i++) {
				TagValuePair tp = bMdo.getMapIndex().decodeType(tps[i]);
				if (tp != null) {
					tags.put(tp.tag, tp.value);
				}
			}
			tps = bMdo.getTypes();
			for (int i = 0; i < tps.length; i++) {
				TagValuePair tp = bMdo.getMapIndex().decodeType(tps[i]);
				if (tp != null) {
					tags.put(tp.tag, tp.value);
				}
			}
			return getRouteKeys(tags);
		}

		private static int getRouteQuantity(Map<String, String> tags, RouteType rType) {
			int q = 0;
			for (String tag : tags.keySet()) {
				if (tag.startsWith(rType.tagPrefix)) {
					int num = Algorithms.extractIntegerNumber(tag);
					if (num > 0 && tag.equals(rType.tagPrefix + num)) {
						q = Math.max(q, num);
					}
				}
			}
			return q;
		}
		
		private static List<RouteKey> getRouteKeys(Map<String, String> tags) {
			List<RouteKey> lst = new ArrayList<RouteKey>();
			for (RouteType routeType : RouteType.values()) {
				int rq = getRouteQuantity(tags, routeType);
				for (int routeIdx = 1; routeIdx <= rq; routeIdx++) {
					String prefix = routeType.tagPrefix + routeIdx;
					RouteKey key = new RouteKey(routeType);
					for (Map.Entry<String, String> e : tags.entrySet()) {
						String tag = e.getKey();
						if (tag.startsWith(prefix)) {
							String tagPart = routeType.tagPrefix + tag.substring(prefix.length());
							if (Algorithms.isEmpty(e.getValue())) {
								key.set.add(tagPart);
							} else {
								key.set.add(tagPart + ROUTE_KEY_VALUE_SEPARATOR + e.getValue());
							}
						}
					}
					lst.add(key);
				}
			}
			return lst;
		}
	}
}