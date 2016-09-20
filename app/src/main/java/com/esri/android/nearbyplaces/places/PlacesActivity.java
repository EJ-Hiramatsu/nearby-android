/* Copyright 2016 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 *
 */

package com.esri.android.nearbyplaces.places;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.esri.android.nearbyplaces.R;
import com.esri.android.nearbyplaces.filter.FilterContract;
import com.esri.android.nearbyplaces.filter.FilterDialogFragment;
import com.esri.android.nearbyplaces.filter.FilterPresenter;
import com.esri.android.nearbyplaces.map.MapActivity;
import com.esri.android.nearbyplaces.util.ActivityUtils;
import com.esri.arcgisruntime.geometry.Envelope;


public class PlacesActivity extends AppCompatActivity implements FilterContract.FilterView,
    ActivityCompat.OnRequestPermissionsResultCallback {

  private static final String TAG = PlacesActivity.class.getSimpleName();
  private static final int PERMISSION_REQUEST_LOCATION = 0;
  private CoordinatorLayout mMainLayout;
  private PlacesPresenter mPresenter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_layout);

    mMainLayout = (CoordinatorLayout) findViewById(R.id.list_coordinator_layout);

    // Set up the toolbar.
    setUpToolbar();

    setUpFragments(savedInstanceState);

    // request location permission
    requestLocationPermission();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu items for use in the action bar
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  /**
   * Set up toolbar
   */
   private void setUpToolbar(){
     Toolbar toolbar = (Toolbar) findViewById(R.id.placeList_toolbar);
     setSupportActionBar(toolbar);

     toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
       @Override public boolean onMenuItemClick(MenuItem item) {
         if (item.getTitle().toString().equalsIgnoreCase(getString(R.string.map_view))){
           // Hide the list, show the map
          showMap(item);
         }
         if (item.getTitle().toString().equalsIgnoreCase(getString(R.string.filter))){
           FilterDialogFragment dialogFragment = new FilterDialogFragment();
           FilterPresenter filterPresenter = new FilterPresenter();
           dialogFragment.setPresenter(filterPresenter);
           dialogFragment.show(getFragmentManager(),"dialog_fragment");

         }
         return false;
       }
     });
   }
  @Override public void onFilterDialogClose(boolean applyFilter) {
    if (applyFilter){
      mPresenter.start();
    }
  }
  public static Intent createMapIntent(Activity a, Envelope envelope){
    Intent intent = new Intent(a, MapActivity.class);
    // Get the extent of search results so map
    // can set viewpoint

    if (envelope != null){
      intent.putExtra("MIN_X", envelope.getXMin());
      intent.putExtra("MIN_Y", envelope.getYMin());
      intent.putExtra("MAX_X", envelope.getXMax());
      intent.putExtra("MAX_Y", envelope.getYMax());
      intent.putExtra("SR", envelope.getSpatialReference().getWKText());
    }
    return  intent;
  }
  private void showMap(MenuItem item){
    Envelope envelope = mPresenter.getExtentForNearbyPlaces();
    Intent intent = createMapIntent(this, envelope);
    startActivity(intent);
  }

  /**
   * Set up fragments
   */
  private void setUpFragments(Bundle savedInstanceState){

    PlacesFragment placesFragment = (PlacesFragment) getSupportFragmentManager().findFragmentById(R.id.recycleView) ;

    if (placesFragment == null){
      // Create the fragment
      placesFragment = PlacesFragment.newInstance();
      ActivityUtils.addFragmentToActivity(
          getSupportFragmentManager(), placesFragment, R.id.list_fragment_container, "list fragment");
    }
    mPresenter = new PlacesPresenter(placesFragment);
  }

  /**
   * Requests the {@link Manifest.permission#ACCESS_FINE_LOCATION}
   * permission. If an additional rationale should be displayed, the user has
   * to launch the request from a SnackBar that includes additional
   * information.
   */

  private void requestLocationPermission() {
    // Permission has not been granted and must be requested.
    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

      // Provide an additional rationale to the user if the permission was
      // not granted
      // and the user would benefit from additional context for the use of
      // the permission.
      // Display a SnackBar with a button to request the missing
      // permission.
      Snackbar.make(mMainLayout, "Location access is required to search for places nearby.", Snackbar.LENGTH_INDEFINITE)
          .setAction("OK", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              // Request the permission
              ActivityCompat.requestPermissions(PlacesActivity.this,
                  new String[]{ Manifest.permission.ACCESS_FINE_LOCATION},
                  PERMISSION_REQUEST_LOCATION);
            }
          }).show();

    } else {
      // Request the permission. The result will be received in
      // onRequestPermissionResult()
      ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION},
          PERMISSION_REQUEST_LOCATION);
    }
  }
  /**
   * Once the app has prompted for permission to access location, the response
   * from the user is handled here. If permission exists to access location
   * check if GPS is available and device is not in airplane mode.
   *
   * @param requestCode
   *            int: The request code passed into requestPermissions
   * @param permissions
   *            String: The requested permission(s).
   * @param grantResults
   *            int: The grant results for the permission(s). This will be
   *            either PERMISSION_GRANTED or PERMISSION_DENIED
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    if (requestCode == PERMISSION_REQUEST_LOCATION) {
      if (grantResults.length != 1 ) {
        // Permission request was denied.
        Snackbar.make(mMainLayout, "Location permission request was denied.", Snackbar.LENGTH_SHORT).show();
      }
    }
  }
}
