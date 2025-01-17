package net.osmand.plus.auto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.Lifecycle;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.views.OsmandMapTileView;

/**
 * Session class for the Navigation sample app.
 */
public class NavigationSession extends Session implements NavigationScreen.Listener, OsmAndLocationListener {
	static final String TAG = NavigationSession.class.getSimpleName();
	static final String URI_SCHEME = "samples";
	static final String URI_HOST = "navigation";

	NavigationScreen navigationScreen;
	RequestPurchaseScreen requestPurchaseScreen;
	SurfaceRenderer navigationCarSurface;
	Action settingsAction;

	private OsmandMapTileView mapView;

	NavigationSession() {
	}

	public NavigationScreen getNavigationScreen() {
		return navigationScreen;
	}

	public SurfaceRenderer getNavigationCarSurface() {
		return navigationCarSurface;
	}

	public OsmandMapTileView getMapView() {
		return mapView;
	}

	public void setMapView(OsmandMapTileView mapView) {
		this.mapView = mapView;
		SurfaceRenderer navigationCarSurface = this.navigationCarSurface;
		if (navigationCarSurface != null) {
			navigationCarSurface.setMapView(mapView);
		}
	}

	public boolean hasStarted() {
		Lifecycle.State state = getLifecycle().getCurrentState();
		return state == Lifecycle.State.STARTED || state == Lifecycle.State.RESUMED;
	}

	public boolean hasSurface() {
		SurfaceRenderer navigationCarSurface = this.navigationCarSurface;
		return navigationCarSurface != null && navigationCarSurface.hasSurface();
	}

	@Override
	@NonNull
	public Screen onCreateScreen(@NonNull Intent intent) {
		Log.i(TAG, "In onCreateScreen()");
		settingsAction =
				new Action.Builder()
						.setIcon(new CarIcon.Builder(
								IconCompat.createWithResource(getCarContext(), R.drawable.ic_action_settings))
								.build())
						.setOnClickListener(() -> getCarContext()
								.getCarService(ScreenManager.class)
								.push(new SettingsScreen(getCarContext())))
						.build();

		navigationCarSurface = new SurfaceRenderer(getCarContext(), getLifecycle());
		if (mapView != null) {
			navigationCarSurface.setMapView(mapView);
		}
		navigationScreen = new NavigationScreen(getCarContext(), settingsAction, this, navigationCarSurface);
		navigationCarSurface.callback = navigationScreen;

		String action = intent.getAction();
		if (CarContext.ACTION_NAVIGATE.equals(action)) {
			CarToast.makeText(
					getCarContext(),
					"Navigation intent: " + intent.getDataString(),
					CarToast.LENGTH_LONG)
					.show();
		}

		OsmandApplication app = (OsmandApplication) getCarContext().getApplicationContext();
		if (!InAppPurchaseHelper.isAndroidAutoAvailable(app)) {
			getCarContext().getCarService(ScreenManager.class).push(navigationScreen);
			requestPurchaseScreen = new RequestPurchaseScreen(getCarContext());
			return requestPurchaseScreen;
		}

		if (ActivityCompat.checkSelfPermission(getCarContext(), Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {
			getCarContext().getCarService(ScreenManager.class).push(navigationScreen);
			return new RequestPermissionScreen(getCarContext(), null);
		}

		return navigationScreen;
	}

	public void onPurchaseDone() {
		OsmandApplication app = (OsmandApplication) getCarContext().getApplicationContext();
		if (requestPurchaseScreen != null && InAppPurchaseHelper.isAndroidAutoAvailable(app)) {
			requestPurchaseScreen.finish();
			requestPurchaseScreen = null;
			app.getOsmandMap().getMapView().setupOpenGLView();
		}
	}

	@Override
	public void onNewIntent(@NonNull Intent intent) {
		Log.i(TAG, "In onNewIntent() " + intent);
		ScreenManager screenManager = getCarContext().getCarService(ScreenManager.class);
		if (CarContext.ACTION_NAVIGATE.equals(intent.getAction())) {
			Uri uri = Uri.parse("http://" + intent.getDataString());
			screenManager.popToRoot();
			String query = uri.getQueryParameter("q");
			if (query == null) {
				query = "";
			}
			screenManager.pushForResult(
					new SearchResultsScreen(
							getCarContext(),
							settingsAction,
							navigationCarSurface,
							query),
					(obj) -> { });

			return;
		}

		// Process the intent from DeepLinkNotificationReceiver. Bring the routing screen back to
		// the
		// top if any other screens were pushed onto it.
		Uri uri = intent.getData();
		if (uri != null
				&& URI_SCHEME.equals(uri.getScheme())
				&& URI_HOST.equals(uri.getSchemeSpecificPart())) {

			/*
			Screen top = screenManager.getTop();
			if (NavigationService.DEEP_LINK_ACTION.equals(uri.getFragment()) && !(top instanceof NavigationScreen)) {
				screenManager.popToRoot();
			}
			 */
		}
	}

	@Override
	public void onCarConfigurationChanged(@NonNull Configuration newConfiguration) {
		if (navigationCarSurface != null) {
			navigationCarSurface.onCarConfigurationChanged();
		}
	}

	@Override
	public void updateNavigation(boolean navigating) {
	}

	@Override
	public void stopNavigation() {
		OsmandApplication app = (OsmandApplication) getCarContext().getApplicationContext();
		if (app != null) {
			app.stopNavigation();
			NavigationScreen navigationScreen = getNavigationScreen();
			if (navigationScreen != null) {
				navigationScreen.stopTrip();
			}
		}
	}

	@Override
	public void updateLocation(Location location) {
		navigationCarSurface.updateLocation(location);
	}
}
