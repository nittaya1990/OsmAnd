package net.osmand.plus.activities;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_AV_NOTES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_BACKUP_RESTORE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_MAP_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_SCREEN_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DASHBOARD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIRECTIONS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIVIDER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DOWNLOAD_MAPS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_FAVORITES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_HELP_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_ITEM_ID_SCHEME;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_LIVE_UPDATES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MAP_MARKERS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MEASURE_DISTANCE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MY_PLACES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_OSM_EDITS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_PLUGINS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SEARCH_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SWITCH_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_TRACKS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_TRAVEL_GUIDES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_TRIP_RECORDING_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ADD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_AVOID_ROAD;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_DIRECTIONS_FROM_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MARKER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MEASURE_DISTANCE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MORE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_SEARCH_NEARBY;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_SHARE_ID;
import static net.osmand.plus.ContextMenuAdapter.PROFILES_CHOSEN_PROFILE_TAG;
import static net.osmand.plus.ContextMenuAdapter.PROFILES_CONTROL_BUTTON_TAG;
import static net.osmand.plus.ContextMenuAdapter.PROFILES_NORMAL_PROFILE_TAG;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.ContextMenuItem.ItemBuilder;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.actions.OsmAndDialogs;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.backup.ui.BackupAndRestoreFragment;
import net.osmand.plus.backup.ui.BackupAuthorizationFragment;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.liveupdates.LiveUpdatesFragment;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment.ContextMenuItemClickListener;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersDialogFragment;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.measurementtool.StartPlanRouteBottomSheet;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.monitoring.TripRecordingBottomSheet;
import net.osmand.plus.monitoring.TripRecordingStartingBottomSheet;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.dialogs.DismissRouteBottomSheetFragment;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.RoutingDataUtils;
import net.osmand.plus.routepreparationmenu.WaypointsFragment;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapActions;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.plus.wikivoyage.WikivoyageWelcomeDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreActivity;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class MapActivityActions extends MapActions implements DialogProvider {

	private static final Log LOG = PlatformUtil.getLog(MapActivityActions.class);

	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_NAME = "name";

	public static final String KEY_ZOOM = "zoom";

	public static final int REQUEST_LOCATION_FOR_DIRECTIONS_NAVIGATION_PERMISSION = 203;

	// Constants for determining the order of items in the additional actions context menu
	public static final int DIRECTIONS_FROM_ITEM_ORDER = 1000;
	public static final int SEARCH_NEAR_ITEM_ORDER = 2000;
	public static final int CHANGE_POSITION_ITEM_ORDER = 3000;
	public static final int EDIT_GPX_WAYPOINT_ITEM_ORDER = 9000;
	public static final int ADD_GPX_WAYPOINT_ITEM_ORDER = 9000;
	public static final int MEASURE_DISTANCE_ITEM_ORDER = 13000;
	public static final int AVOID_ROAD_ITEM_ORDER = 14000;

	private static final int DIALOG_ADD_FAVORITE = 100;
	private static final int DIALOG_REPLACE_FAVORITE = 101;
	private static final int DIALOG_ADD_WAYPOINT = 102;
	private static final int DIALOG_RELOAD_TITLE = 103;

	private static final int DIALOG_SAVE_DIRECTIONS = 106;

	private static final int DRAWER_MODE_NORMAL = 0;
	private static final int DRAWER_MODE_SWITCH_PROFILE = 1;

	// make static
	private static Bundle dialogBundle = new Bundle();

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapActivity mapActivity;
	private final RoutingDataUtils routingDataUtils;

	@NonNull
	private final ImageView drawerLogoHeader;

	private int drawerMode = DRAWER_MODE_NORMAL;

	public MapActivityActions(@NonNull MapActivity mapActivity) {
		super(mapActivity.getMyApplication());
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();
		this.mapActivity = mapActivity;
		this.routingDataUtils = new RoutingDataUtils(app);
		this.drawerLogoHeader = new ImageView(mapActivity);
		this.drawerLogoHeader.setPadding(-AndroidUtils.dpToPx(mapActivity, 8f),
				AndroidUtils.dpToPx(mapActivity, 16f), 0, 0);
	}

	public void addAsTarget(double latitude, double longitude, PointDescription pd) {
		TargetPointsHelper targets = app.getTargetPointsHelper();
		targets.navigateToPoint(new LatLon(latitude, longitude), true, targets.getIntermediatePoints().size() + 1, pd);
		openIntermediatePointsDialog();
	}


	public void addMapMarker(double latitude, double longitude, PointDescription pd, @Nullable String mapObjectName) {
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		markersHelper.addMapMarker(new LatLon(latitude, longitude), pd, mapObjectName);
	}

	public void editWaypoints() {
		openIntermediatePointsDialog();
	}

	private Bundle enhance(Bundle aBundle, double latitude, double longitude, String name) {
		aBundle.putDouble(KEY_LATITUDE, latitude);
		aBundle.putDouble(KEY_LONGITUDE, longitude);
		aBundle.putString(KEY_NAME, name);
		return aBundle;
	}

	private Bundle enhance(Bundle bundle, double latitude, double longitude, final int zoom) {
		bundle.putDouble(KEY_LATITUDE, latitude);
		bundle.putDouble(KEY_LONGITUDE, longitude);
		bundle.putInt(KEY_ZOOM, zoom);
		return bundle;
	}

	private Dialog createAddWaypointDialog(final Bundle args) {
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, nightMode));
		builder.setTitle(R.string.add_waypoint_dialog_title);

		View view = UiUtilities.getInflater(mapActivity, nightMode).inflate(R.layout.add_gpx_point_dialog, null);
		final EditText editText = (EditText) view.findViewById(android.R.id.edit);
		builder.setView(view);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				double latitude = args.getDouble(KEY_LATITUDE);
				double longitude = args.getDouble(KEY_LONGITUDE);
				String name = editText.getText().toString();
				SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
				savingTrackHelper.insertPointData(latitude, longitude, System.currentTimeMillis(), null, name, null, 0);
				Toast.makeText(mapActivity, MessageFormat.format(getString(R.string.add_waypoint_dialog_added), name), Toast.LENGTH_SHORT)
						.show();
				dialog.dismiss();
			}
		});
		final AlertDialog alertDialog = builder.create();
		editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});
		return alertDialog;
	}

	public void reloadTile(final int zoom, final double latitude, final double longitude) {
		enhance(dialogBundle, latitude, longitude, zoom);
		mapActivity.showDialog(DIALOG_RELOAD_TITLE);
	}

	protected String getString(int res) {
		return mapActivity.getString(res);
	}

	protected void showToast(final String msg) {
		mapActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mapActivity, msg, Toast.LENGTH_LONG).show();
			}
		});
	}

	public void saveDirections() {
		mapActivity.showDialog(DIALOG_SAVE_DIRECTIONS);
	}

	public static Dialog createSaveDirections(Activity activity, RoutingHelper routingHelper) {
		final OsmandApplication app = ((OsmandApplication) activity.getApplication());
		final File fileDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		final Dialog dlg = new Dialog(activity);
		dlg.setTitle(R.string.shared_string_save_as_gpx);
		dlg.setContentView(R.layout.save_directions_dialog);
		final EditText edit = (EditText) dlg.findViewById(R.id.FileNameEdit);

		final GPXRouteParamsBuilder rp = routingHelper.getCurrentGPXRoute();
		final String editText;
		if (rp == null || rp.getFile() == null || rp.getFile().path == null) {
			editText = "_" + MessageFormat.format("{0,date,yyyy-MM-dd}", new Date()) + "_";
		} else {
			editText = new File(rp.getFile().path).getName();
		}
		edit.setText(editText);

		dlg.findViewById(R.id.Save).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = edit.getText().toString();
				//noinspection ResultOfMethodCallIgnored
				fileDir.mkdirs();
				File toSave = fileDir;
				if (name.length() > 0) {
					if (!name.endsWith(GPX_FILE_EXT)) {
						name += GPX_FILE_EXT;
					}
					toSave = new File(fileDir, name);
				}
				if (toSave.exists()) {
					dlg.findViewById(R.id.DuplicateFileName).setVisibility(View.VISIBLE);
				} else {
					dlg.dismiss();
					new SaveDirectionsAsyncTask(app, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, toSave);
				}
			}
		});

		dlg.findViewById(R.id.Cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dlg.dismiss();
			}
		});


		return dlg;
	}

	public static class SaveDirectionsAsyncTask extends AsyncTask<File, Void, GPXFile> {

		private final OsmandApplication app;
		boolean showOnMap;

		public SaveDirectionsAsyncTask(OsmandApplication app, boolean showOnMap) {
			this.app = app;
			this.showOnMap = showOnMap;
		}

		@Override
		protected GPXFile doInBackground(File... params) {
			if (params.length > 0) {
				File file = params[0];
				String fileName = Algorithms.getFileNameWithoutExtension(file);
				GPXFile gpx = app.getRoutingHelper().generateGPXFileWithRoute(fileName);
				gpx.error = GPXUtilities.writeGpxFile(file, gpx);
				return gpx;
			}
			return null;
		}

		@Override
		protected void onPostExecute(GPXFile gpxFile) {
			if (gpxFile.error == null) {
				app.getSelectedGpxHelper().selectGpxFile(gpxFile, showOnMap, false);
				String result = app.getString(R.string.route_successfully_saved_at, gpxFile.tracks.get(0).name);
				Toast.makeText(app, result, Toast.LENGTH_LONG).show();
			} else {
				String errorMessage = gpxFile.error.getMessage();
				if (errorMessage == null) {
					errorMessage = app.getString(R.string.error_occurred_saving_gpx);
				}
				Toast.makeText(app, errorMessage, Toast.LENGTH_LONG).show();
			}
		}
	}

	public void addActionsToAdapter(final double latitude,
									final double longitude,
									final ContextMenuAdapter adapter,
									Object selectedObj,
									boolean configureMenu) {
		ItemBuilder itemBuilder = new ItemBuilder();

		adapter.addItem(itemBuilder
				.setTitleId(selectedObj instanceof FavouritePoint ? R.string.favourites_context_menu_edit : R.string.shared_string_add, mapActivity)
				.setId(MAP_CONTEXT_MENU_ADD_ID)
				.setIcon(selectedObj instanceof FavouritePoint ? R.drawable.ic_action_edit_dark : R.drawable.ic_action_favorite_stroke)
				.setOrder(10)
				.createItem());
		adapter.addItem(itemBuilder
				.setTitleId(selectedObj instanceof MapMarker ? R.string.shared_string_edit : R.string.shared_string_marker, mapActivity)
				.setId(MAP_CONTEXT_MENU_MARKER_ID)
				.setOrder(20)
				.setIcon(selectedObj instanceof MapMarker ? R.drawable.ic_action_edit_dark : R.drawable.ic_action_flag_stroke)
				.createItem());
		adapter.addItem(itemBuilder
				.setTitleId(R.string.shared_string_share, mapActivity)
				.setId(MAP_CONTEXT_MENU_SHARE_ID)
				.setOrder(30)
				.setIcon(R.drawable.ic_action_gshare_dark)
				.createItem());
		adapter.addItem(itemBuilder
				.setTitleId(R.string.shared_string_actions, mapActivity)
				.setId(MAP_CONTEXT_MENU_MORE_ID)
				.setIcon(R.drawable.ic_actions_menu)
				.setOrder(40)
				.createItem());

		adapter.addItem(itemBuilder
				.setTitleId(R.string.context_menu_item_directions_from, mapActivity)
				.setId(MAP_CONTEXT_MENU_DIRECTIONS_FROM_ID)
				.setIcon(R.drawable.ic_action_route_direction_from_here)
				.setOrder(DIRECTIONS_FROM_ITEM_ORDER)
				.createItem());
		adapter.addItem(itemBuilder
				.setTitleId(R.string.context_menu_item_search, mapActivity)
				.setId(MAP_CONTEXT_MENU_SEARCH_NEARBY)
				.setIcon(R.drawable.ic_action_search_dark)
				.setOrder(SEARCH_NEAR_ITEM_ORDER)
				.createItem());

		OsmandPlugin.registerMapContextMenu(mapActivity, latitude, longitude, adapter, selectedObj, configureMenu);

		ItemClickListener listener = new ItemClickListener() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int resId, int pos, boolean isChecked, int[] viewCoordinates) {
				if (resId == R.string.context_menu_item_add_waypoint) {
					mapActivity.getContextMenu().addWptPt();
				} else if (resId == R.string.context_menu_item_edit_waypoint) {
					mapActivity.getContextMenu().editWptPt();
				}
				return true;
			}
		};

		ContextMenuItem editGpxItem = new ItemBuilder()
				.setTitleId(R.string.context_menu_item_edit_waypoint, mapActivity)
				.setId(MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT)
				.setIcon(R.drawable.ic_action_edit_dark)
				.setOrder(EDIT_GPX_WAYPOINT_ITEM_ORDER)
				.setListener(listener).createItem();
		ContextMenuItem addGpxItem = new ItemBuilder()
				.setTitleId(R.string.context_menu_item_add_waypoint, mapActivity)
				.setId(MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT)
				.setIcon(R.drawable.ic_action_gnew_label_dark)
				.setOrder(ADD_GPX_WAYPOINT_ITEM_ORDER)
				.setListener(listener).createItem();

		if (configureMenu) {
			adapter.addItem(addGpxItem);
		} else if (selectedObj instanceof WptPt
				&& app.getSelectedGpxHelper().getSelectedGPXFile((WptPt) selectedObj) != null) {
			adapter.addItem(editGpxItem);
		} else if (!app.getSelectedGpxHelper().getSelectedGPXFiles().isEmpty()
				|| (OsmandPlugin.isActive(OsmandMonitoringPlugin.class))) {
			adapter.addItem(addGpxItem);
		}

		adapter.addItem(itemBuilder
				.setTitleId(R.string.plan_route, mapActivity)
				.setId(MAP_CONTEXT_MENU_MEASURE_DISTANCE)
				.setIcon(R.drawable.ic_action_ruler)
				.setOrder(MEASURE_DISTANCE_ITEM_ORDER)
				.createItem());

		adapter.addItem(itemBuilder
				.setTitleId(R.string.avoid_road, mapActivity)
				.setId(MAP_CONTEXT_MENU_AVOID_ROAD)
				.setIcon(R.drawable.ic_action_alert)
				.setOrder(AVOID_ROAD_ITEM_ORDER)
				.createItem());
	}

	public void contextMenuPoint(final double latitude, final double longitude, final ContextMenuAdapter iadapter, Object selectedObj) {
		ContextMenuAdapter adapter = iadapter == null ? new ContextMenuAdapter(app) : iadapter;
		addActionsToAdapter(latitude, longitude, adapter, selectedObj, false);
		showAdditionalActionsFragment(adapter, getContextMenuItemClickListener(latitude, longitude, adapter));
	}

	public void showAdditionalActionsFragment(final ContextMenuAdapter adapter, AdditionalActionsBottomSheetDialogFragment.ContextMenuItemClickListener listener) {
		AdditionalActionsBottomSheetDialogFragment actionsBottomSheetDialogFragment = new AdditionalActionsBottomSheetDialogFragment();
		actionsBottomSheetDialogFragment.setAdapter(adapter, listener);
		actionsBottomSheetDialogFragment.show(mapActivity.getSupportFragmentManager(), AdditionalActionsBottomSheetDialogFragment.TAG);
	}

	public ContextMenuItemClickListener getContextMenuItemClickListener(final double latitude, final double longitude, final ContextMenuAdapter adapter) {
		final ArrayAdapter<ContextMenuItem> listAdapter = adapter.createListAdapter(mapActivity, settings.isLightContent());

		return new AdditionalActionsBottomSheetDialogFragment.ContextMenuItemClickListener() {
			@Override
			public void onItemClick(int position) {
				ContextMenuItem item = adapter.getItem(position);
				int standardId = item.getTitleId();
				ItemClickListener click = item.getItemClickListener();
				if (click != null) {
					click.onContextMenuClick(listAdapter, standardId, position, false, null);
				} else if (standardId == R.string.context_menu_item_search) {
					mapActivity.showQuickSearch(latitude, longitude);
				} else if (standardId == R.string.context_menu_item_directions_from) {
					//if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
					enterDirectionsFromPoint(latitude, longitude);
					//} else {
					//	ActivityCompat.requestPermissions(mapActivity,
					//			new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					//			REQUEST_LOCATION_FOR_DIRECTIONS_NAVIGATION_PERMISSION);
					//}
				} else if (standardId == R.string.plan_route) {
					mapActivity.getContextMenu().close();
					MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager(), new LatLon(latitude, longitude));
				} else if (standardId == R.string.avoid_road) {
					app.getAvoidSpecificRoads().addImpassableRoad(mapActivity, new LatLon(latitude, longitude), true, false, null);
				} else if (standardId == R.string.shared_string_add || standardId == R.string.favourites_context_menu_edit) {
					mapActivity.getContextMenu().hide();
					mapActivity.getContextMenu().buttonFavoritePressed();
				} else if (standardId == R.string.shared_string_marker || standardId == R.string.shared_string_edit) {
					mapActivity.getContextMenu().buttonWaypointPressed();
				} else if (standardId == R.string.shared_string_share) {
					mapActivity.getContextMenu().buttonSharePressed();
				}
			}
		};
	}

	public void enterDirectionsFromPoint(final double latitude, final double longitude) {
		mapActivity.getContextMenu().hide();
		if (!mapActivity.getRoutingHelper().isFollowingMode() && !mapActivity.getRoutingHelper().isRoutePlanningMode()) {
			enterRoutePlanningMode(new LatLon(latitude, longitude), mapActivity.getContextMenu().getPointDescription());
		} else {
			app.getTargetPointsHelper().setStartPoint(new LatLon(latitude, longitude),
					true, mapActivity.getContextMenu().getPointDescription());
		}
	}

	@Override
	public boolean hasUiContext() {
		return true;
	}

	@Override
	public void enterRoutePlanningModeGivenGpx(GPXFile gpxFile, ApplicationMode appMode, LatLon from,
											   PointDescription fromName, boolean useIntermediatePointsByDefault,
											   boolean showMenu, int menuState) {
		super.enterRoutePlanningModeGivenGpx(gpxFile, appMode, from, fromName, useIntermediatePointsByDefault,
				showMenu, menuState);
		if (showMenu) {
			app.getOsmandMap().getMapLayers().getMapControlsLayer().showRouteInfoMenu(menuState);
		}
		if (!settings.SPEED_CAMERAS_ALERT_SHOWED.get()) {
			SpeedCamerasBottomSheet.showInstance(mapActivity.getSupportFragmentManager(), null);
		}
	}

	@Override
	public void recalculateRoute(boolean showDialog) {
		super.recalculateRoute(showDialog);
		if (showDialog) {
			app.getOsmandMap().getMapLayers().getMapControlsLayer().showRouteInfoMenu();
		}
	}

	@Override
	protected void initVoiceCommandPlayer(@NonNull ApplicationMode mode, boolean showMenu) {
		app.initVoiceCommandPlayer(mapActivity, mode, null, true, false, false, showMenu);
	}

	public void contextMenuPoint(final double latitude, final double longitude) {
		contextMenuPoint(latitude, longitude, null, null);
	}

	private Dialog createReloadTitleDialog(final Bundle args) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setMessage(R.string.context_menu_item_update_map_confirm);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		final OsmandMapTileView mapView = mapActivity.getMapView();
		builder.setPositiveButton(R.string.context_menu_item_update_map, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int zoom = args.getInt(KEY_ZOOM);
				BaseMapLayer mainLayer = mapView.getMainLayer();
				if (!(mainLayer instanceof MapTileLayer) || !((MapTileLayer) mainLayer).isVisible()) {
					Toast.makeText(mapActivity, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
					return;
				}
				final ITileSource mapSource = ((MapTileLayer) mainLayer).getMap();
				if (mapSource == null || !mapSource.couldBeDownloadedFromInternet()) {
					Toast.makeText(mapActivity, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
					return;
				}
				final RotatedTileBox tb = mapView.getCurrentRotatedTileBox();
				final QuadRect tilesRect = tb.getTileBounds();
				long requestTimestamp = System.currentTimeMillis();
				int left = (int) Math.floor(tilesRect.left);
				int top = (int) Math.floor(tilesRect.top);
				int width = (int) (Math.ceil(tilesRect.right) - left);
				int height = (int) (Math.ceil(tilesRect.bottom) - top);
				for (int i = 0; i < width; i++) {
					for (int j = 0; j < height; j++) {
						((OsmandApplication) mapActivity.getApplication()).getResourceManager().
								clearTileForMap(null, mapSource, i + left, j + top, zoom, requestTimestamp);
					}
				}


				mapView.refreshMap();
			}
		});
		return builder.create();
	}


	@Override
	public Dialog onCreateDialog(int id) {
		Bundle args = dialogBundle;
		switch (id) {
			case DIALOG_ADD_FAVORITE:
				return FavoriteDialogs.createAddFavouriteDialog(mapActivity, args);
			case DIALOG_REPLACE_FAVORITE:
				return FavoriteDialogs.createReplaceFavouriteDialog(mapActivity, args);
			case DIALOG_ADD_WAYPOINT:
				return createAddWaypointDialog(args);
			case DIALOG_RELOAD_TITLE:
				return createReloadTitleDialog(args);
			case DIALOG_SAVE_DIRECTIONS:
				return createSaveDirections(mapActivity, mapActivity.getRoutingHelper());
		}
		return OsmAndDialogs.createDialog(id, mapActivity, args);
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		Bundle args = dialogBundle;
		switch (id) {
			case DIALOG_ADD_FAVORITE:
				FavoriteDialogs.prepareAddFavouriteDialog(mapActivity, dialog, args,
						args.getDouble(KEY_LATITUDE), args.getDouble(KEY_LONGITUDE),
						new PointDescription(PointDescription.POINT_TYPE_FAVORITE, args.getString(KEY_NAME)));
				break;
			case DIALOG_ADD_WAYPOINT:
				EditText v = (EditText) dialog.getWindow().findViewById(android.R.id.edit);
				v.setPadding(5, 0, 5, 0);
				if (args.getString(KEY_NAME) != null) {
					v.setText(args.getString(KEY_NAME));
					v.selectAll();
				} else {
					v.setText("");
				}
				break;
		}
	}

	public ContextMenuAdapter createMainOptionsMenu() {
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		ContextMenuAdapter optionsMenuHelper = new ContextMenuAdapter(app);
		optionsMenuHelper.setNightMode(nightMode);

		if (drawerMode == DRAWER_MODE_SWITCH_PROFILE) {
			return createSwitchProfileOptionsMenu(app, optionsMenuHelper, nightMode);
		}
		return createNormalOptionsMenu(app, optionsMenuHelper, nightMode);
	}

	private ContextMenuAdapter createSwitchProfileOptionsMenu(final OsmandApplication app, ContextMenuAdapter optionsMenuHelper, boolean nightMode) {
		drawerMode = DRAWER_MODE_NORMAL;
		createProfilesController(app, optionsMenuHelper, nightMode, true);

		List<ApplicationMode> activeModes = ApplicationMode.values(app);
		ApplicationMode currentMode = app.getSettings().APPLICATION_MODE.get();

		String modeDescription;

		Map<String, ProfileDataObject> profilesObjects = routingDataUtils.getRoutingProfiles();
		for (final ApplicationMode appMode : activeModes) {
			if (appMode.isCustomProfile()) {
				modeDescription = getProfileDescription(app, appMode, profilesObjects, getString(R.string.profile_type_user_string));
			} else {
				modeDescription = getProfileDescription(app, appMode, profilesObjects, getString(R.string.profile_type_osmand_string));
			}

			int tag = currentMode.equals(appMode) ? PROFILES_CHOSEN_PROFILE_TAG : PROFILES_NORMAL_PROFILE_TAG;

			optionsMenuHelper.addItem(new ItemBuilder().setLayout(R.layout.profile_list_item)
					.setIcon(appMode.getIconRes())
					.setColor(appMode.getProfileColor(nightMode))
					.setTag(tag)
					.setTitle(appMode.toHumanString())
					.setDescription(modeDescription)
					.setListener(new ItemClickListener() {
						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
							app.getSettings().setApplicationMode(appMode);
							updateDrawerMenu();
							return false;
						}
					})
					.createItem());
		}

		optionsMenuHelper.addItem(new ItemBuilder().setLayout(R.layout.profile_list_item)
				.setColor(ColorUtilities.getActiveColor(app, nightMode))
				.setTag(PROFILES_CONTROL_BUTTON_TAG)
				.setTitle(getString(R.string.shared_string_manage))
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						BaseSettingsFragment.showInstance(mapActivity, BaseSettingsFragment.SettingsScreenType.MAIN_SETTINGS);
						return true;
					}
				})
				.createItem());

		return optionsMenuHelper;
	}

	private ContextMenuAdapter createNormalOptionsMenu(final OsmandApplication app, ContextMenuAdapter optionsMenuHelper, boolean nightMode) {

		createProfilesController(app, optionsMenuHelper, nightMode, false);

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.home, mapActivity)
				.setId(DRAWER_DASHBOARD_ID)
				.setIcon(R.drawable.ic_dashboard)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_dashboard_open");
						MapActivity.clearPrevActivityIntent();
						mapActivity.closeDrawer();
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.DASHBOARD, viewCoordinates);
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.map_markers, mapActivity)
				.setId(DRAWER_MAP_MARKERS_ID)
				.setIcon(R.drawable.ic_action_flag)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_markers_open");
						MapActivity.clearPrevActivityIntent();
						MapMarkersDialogFragment.showInstance(mapActivity);
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.shared_string_my_places, mapActivity)
				.setId(DRAWER_MY_PLACES_ID)
				.setIcon(R.drawable.ic_action_favorite)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_myplaces_open");
						Intent newIntent = new Intent(mapActivity, app.getAppCustomization()
								.getFavoritesActivity());
						newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(newIntent);
						return true;
					}
				}).createItem());

		addMyPlacesTabToDrawer(optionsMenuHelper, R.string.shared_string_my_favorites,
				R.drawable.ic_action_folder_favorites, DRAWER_FAVORITES_ID);
		addMyPlacesTabToDrawer(optionsMenuHelper, R.string.shared_string_tracks,
				R.drawable.ic_action_folder_tracks, DRAWER_TRACKS_ID);
		if (OsmandPlugin.isActive(AudioVideoNotesPlugin.class)) {
			addMyPlacesTabToDrawer(optionsMenuHelper, R.string.notes,
					R.drawable.ic_action_folder_av_notes, DRAWER_AV_NOTES_ID);
		}
		if (OsmandPlugin.isActive(OsmEditingPlugin.class)) {
			addMyPlacesTabToDrawer(optionsMenuHelper, R.string.osm_edits,
					R.drawable.ic_action_folder_osm_notes, DRAWER_OSM_EDITS_ID);
		}

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.backup_and_restore, mapActivity)
				.setId(DRAWER_BACKUP_RESTORE_ID)
				.setIcon(R.drawable.ic_action_cloud_upload)
				.setListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
					app.logEvent("drawer_backup_restore_open");
					if (app.getBackupHelper().isRegistered()) {
						BackupAndRestoreFragment.showInstance(mapActivity.getSupportFragmentManager());
					} else {
						BackupAuthorizationFragment.showInstance(mapActivity.getSupportFragmentManager());
					}
					return true;
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.search_button, mapActivity)
				.setId(DRAWER_SEARCH_ID)
				.setIcon(R.drawable.ic_action_search_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_search_open");
						mapActivity.showQuickSearch(MapActivity.ShowQuickSearchMode.NEW_IF_EXPIRED, false);
						return true;
					}
				}).createItem());

		final OsmandMonitoringPlugin monitoringPlugin = OsmandPlugin.getActivePlugin(OsmandMonitoringPlugin.class);
		if (monitoringPlugin != null) {
			optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.map_widget_monitoring, mapActivity)
					.setId(DRAWER_TRIP_RECORDING_ID)
					.setIcon(R.drawable.ic_action_track_recordable)
					.setListener(new ItemClickListener() {
						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
							app.logEvent("trip_recording_open");
							MapActivity.clearPrevActivityIntent();
							if (monitoringPlugin.hasDataToSave() || monitoringPlugin.wasTrackMonitored()) {
								TripRecordingBottomSheet.showInstance(mapActivity.getSupportFragmentManager());
							} else {
								TripRecordingStartingBottomSheet.showTripRecordingDialog(mapActivity.getSupportFragmentManager(), app);
							}
							return true;
						}
					}).createItem());
		}

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.shared_string_navigation, mapActivity)
				.setId(DRAWER_DIRECTIONS_ID)
				.setIcon(R.drawable.ic_action_gdirections_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_directions_open");
						MapControlsLayer mapControlsLayer = mapActivity.getMapLayers().getMapControlsLayer();
						if (mapControlsLayer != null) {
							mapControlsLayer.doRoute(false);
						}
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.configure_map, mapActivity)
				.setId(DRAWER_CONFIGURE_MAP_ID)
				.setIcon(R.drawable.ic_action_layers)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_config_map_open");
						MapActivity.clearPrevActivityIntent();
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP, viewCoordinates);
						return false;
					}
				}).createItem());

		String d = getString(R.string.welmode_download_maps);
		if (app.getDownloadThread().getIndexes().isDownloadedFromInternet) {
			List<IndexItem> updt = app.getDownloadThread().getIndexes().getItemsToUpdate();
			if (updt != null && updt.size() > 0) {
				d += " (" + updt.size() + ")";
			}
		}
		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.welmode_download_maps, null)
				.setId(DRAWER_DOWNLOAD_MAPS_ID)
				.setTitle(d).setIcon(R.drawable.ic_type_archive)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_download_maps_open");
						Intent newIntent = new Intent(mapActivity, app.getAppCustomization().getDownloadActivity());
						newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(newIntent);
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.live_updates, mapActivity)
				.setId(DRAWER_LIVE_UPDATES_ID)
				.setIcon(R.drawable.ic_action_map_update)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						LiveUpdatesFragment.showInstance(mapActivity.getSupportFragmentManager(), null);
						return true;
					}
				}).createItem()
		);

		optionsMenuHelper.addItem(new ItemBuilder().setTitle(getString(R.string.shared_string_travel_guides) + " (Beta)")
				.setId(DRAWER_TRAVEL_GUIDES_ID)
				.setIcon(R.drawable.ic_action_travel)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						MapActivity.clearPrevActivityIntent();
						TravelHelper travelHelper = app.getTravelHelper();
						travelHelper.initializeDataOnAppStartup();
						if (!travelHelper.isAnyTravelBookPresent() && !travelHelper.getBookmarksHelper().hasSavedArticles()) {
							WikivoyageWelcomeDialogFragment.showInstance(mapActivity.getSupportFragmentManager());
						} else {
							Intent intent = new Intent(mapActivity, WikivoyageExploreActivity.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							mapActivity.startActivity(intent);
						}
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.plan_route, mapActivity)
				.setId(DRAWER_MEASURE_DISTANCE_ID)
				.setIcon(R.drawable.ic_action_plan_route)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						StartPlanRouteBottomSheet.showInstance(mapActivity.getSupportFragmentManager());
						return true;
					}
				}).createItem());

		app.getAidlApi().registerNavDrawerItems(mapActivity, optionsMenuHelper);

		optionsMenuHelper.addItem(new ItemBuilder().setLayout(R.layout.drawer_divider)
				.setId(DRAWER_DIVIDER_ID)
				.createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.layer_map_appearance, mapActivity)
				.setId(DRAWER_CONFIGURE_SCREEN_ID)
				.setIcon(R.drawable.ic_configure_screen_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_config_screen_open");
						MapActivity.clearPrevActivityIntent();
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_SCREEN, viewCoordinates);
						return false;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.prefs_plugins, mapActivity)
				.setId(DRAWER_PLUGINS_ID)
				.setIcon(R.drawable.ic_extension_dark)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_plugins_open");
						PluginsFragment.showInstance(mapActivity.getSupportFragmentManager());
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitle(getString(R.string.shared_string_settings))
				.setId(DRAWER_SETTINGS_ID + ".new")
				.setIcon(R.drawable.ic_action_settings)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_settings_new_open");
						mapActivity.showSettings();
						return true;
					}
				}).createItem());

		optionsMenuHelper.addItem(new ItemBuilder().setTitleId(R.string.shared_string_help, mapActivity)
				.setId(DRAWER_HELP_ID)
				.setIcon(R.drawable.ic_action_help)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						app.logEvent("drawer_help_open");
						Intent intent = new Intent(mapActivity, HelpActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						mapActivity.startActivity(intent);
						return true;
					}
				}).createItem());

		//////////// Others
		OsmandPlugin.registerOptionsMenu(mapActivity, optionsMenuHelper);

		return optionsMenuHelper;
	}

	private void createProfilesController(final OsmandApplication app, ContextMenuAdapter optionsMenuHelper, boolean nightMode, boolean listExpanded) {
		//switch profile button
		ApplicationMode currentMode = app.getSettings().APPLICATION_MODE.get();
		String modeDescription;
		Map<String, ProfileDataObject> profilesObjects = routingDataUtils.getRoutingProfiles();
		if (currentMode.isCustomProfile()) {
			modeDescription = getProfileDescription(app, currentMode, profilesObjects, getString(R.string.profile_type_user_string));
		} else {
			modeDescription = getProfileDescription(app, currentMode, profilesObjects, getString(R.string.profile_type_osmand_string));
		}

		int icArrowResId = listExpanded ? R.drawable.ic_action_arrow_drop_up : R.drawable.ic_action_arrow_drop_down;
		final int nextMode = listExpanded ? DRAWER_MODE_NORMAL : DRAWER_MODE_SWITCH_PROFILE;
		optionsMenuHelper.addItem(new ItemBuilder().setLayout(R.layout.main_menu_drawer_btn_switch_profile)
				.setId(DRAWER_SWITCH_PROFILE_ID)
				.setIcon(currentMode.getIconRes())
				.setSecondaryIcon(icArrowResId)
				.setColor(currentMode.getProfileColor(nightMode))
				.setTitle(currentMode.toHumanString())
				.setDescription(modeDescription)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						drawerMode = nextMode;
						updateDrawerMenu();
						return false;
					}
				})
				.createItem());
		optionsMenuHelper.addItem(new ItemBuilder().setLayout(R.layout.main_menu_drawer_btn_configure_profile)
				.setId(DRAWER_CONFIGURE_PROFILE_ID)
				.setColor(currentMode.getProfileColor(nightMode))
				.setTitle(getString(R.string.configure_profile))
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						mapActivity.dismissSettingsScreens();
						BaseSettingsFragment.showInstance(mapActivity, BaseSettingsFragment.SettingsScreenType.CONFIGURE_PROFILE);
						return true;
					}
				})
				.createItem());
	}

	private String getProfileDescription(OsmandApplication app, ApplicationMode mode,
										 Map<String, ProfileDataObject> profilesObjects, String defaultDescription) {
		String description = defaultDescription;

		String routingProfileKey = mode.getRoutingProfile();
		if (!Algorithms.isEmpty(routingProfileKey)) {
			ProfileDataObject profileDataObject = profilesObjects.get(routingProfileKey);
			if (profileDataObject != null) {
				description = String.format(app.getString(R.string.profile_type_descr_string),
						Algorithms.capitalizeFirstLetterAndLowercase(profileDataObject.getName()));
			}
		}
		return description;
	}

	private void addMyPlacesTabToDrawer(ContextMenuAdapter adapter, @StringRes int titleRes,
										@DrawableRes int iconRes, String drawerId) {
		adapter.addItem(new ItemBuilder().setTitleId(titleRes, mapActivity)
				.setId(drawerId)
				.setIcon(iconRes)
				.setListener((adapter1, itemId, position, isChecked, viewCoordinates) -> {
					String itemLogName = drawerId.replace(DRAWER_ITEM_ID_SCHEME, "");
					app.logEvent("drawer_" + itemLogName + "_open");
					Intent newIntent = new Intent(mapActivity, app.getAppCustomization()
							.getFavoritesActivity());
					newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					settings.FAVORITES_TAB.set(titleRes);
					mapActivity.startActivity(newIntent);
					return true;

				})
				.createItem());
	}

	public void openIntermediatePointsDialog() {
		mapActivity.hideContextAndRouteInfoMenues();
		WaypointsFragment.showInstance(mapActivity.getSupportFragmentManager());
	}

	@Override
	public void stopNavigationWithoutConfirm() {
		super.stopNavigationWithoutConfirm();
		mapActivity.updateApplicationModeSettings();
	}

	public void stopNavigationActionConfirm(@Nullable OnDismissListener listener) {
		stopNavigationActionConfirm(listener, null);
	}

	public void stopNavigationActionConfirm(@Nullable OnDismissListener listener, @Nullable Runnable onStopAction) {
		DismissRouteBottomSheetFragment.showInstance(mapActivity.getSupportFragmentManager(), listener, onStopAction);
	}

	public void whereAmIDialog() {
		final List<String> items = new ArrayList<>();
		items.add(getString(R.string.show_location));
		items.add(getString(R.string.shared_string_show_details));
		AlertDialog.Builder menu = new AlertDialog.Builder(mapActivity);
		menu.setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				dialog.dismiss();
				switch (item) {
					case 0:
						mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
						break;
					case 1:
						OsmAndLocationProvider locationProvider = app.getLocationProvider();
						locationProvider.showNavigationInfo(mapActivity.getPointToNavigate(), mapActivity);
						break;
					default:
						break;
				}
			}
		});
		menu.show();
	}

	protected void updateDrawerMenu() {
		final boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		final ListView menuItemsListView = (ListView) mapActivity.findViewById(R.id.menuItems);
		menuItemsListView.setBackgroundColor(ColorUtilities.getListBgColor(mapActivity, nightMode));
		menuItemsListView.removeHeaderView(drawerLogoHeader);
		Bitmap navDrawerLogo = app.getAppCustomization().getNavDrawerLogo();

		if (navDrawerLogo != null) {
			drawerLogoHeader.setImageBitmap(navDrawerLogo);
			menuItemsListView.addHeaderView(drawerLogoHeader);
		}
		menuItemsListView.setDivider(null);
		final ContextMenuAdapter contextMenuAdapter = createMainOptionsMenu();
		contextMenuAdapter.setDefaultLayoutId(R.layout.simple_list_menu_item);
		final ArrayAdapter<ContextMenuItem> simpleListAdapter = contextMenuAdapter.createListAdapter(mapActivity,
				!nightMode);
		menuItemsListView.setAdapter(simpleListAdapter);
		menuItemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mapActivity.dismissCardDialog();
				boolean hasHeader = menuItemsListView.getHeaderViewsCount() > 0;
				boolean hasFooter = menuItemsListView.getFooterViewsCount() > 0;
				if (hasHeader && position == 0 || (hasFooter && position == menuItemsListView.getCount() - 1)) {
					String drawerLogoParams = app.getAppCustomization().getNavDrawerLogoUrl();
					if (!Algorithms.isEmpty(drawerLogoParams)) {
						WikipediaDialogFragment.showFullArticle(mapActivity, Uri.parse(drawerLogoParams), nightMode);
					}
				} else {
					position -= menuItemsListView.getHeaderViewsCount();
					ContextMenuItem item = contextMenuAdapter.getItem(position);
					ItemClickListener click = item.getItemClickListener();
					if (click != null && click.onContextMenuClick(simpleListAdapter, item.getTitleId(),
							position, false, AndroidUtils.getCenterViewCoordinates(view))) {
						mapActivity.closeDrawer();
					}
				}
			}
		});
	}
}
