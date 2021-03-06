/*
 *  Flingbox - An OpenSource physics sandbox for Google's Android
 *  Copyright (C) 2009  Jon Ander Peñalba & Endika Gutiérrez
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.eside.flingbox;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ZoomControls;

import edu.eside.flingbox.scene.Scene;
import edu.eside.flingbox.xml.XmlExporter;
import edu.eside.flingbox.xml.XmlImporter;

/**
 * Flingbox main activity. Shows scene at full screen.
 */
public class FlingboxActivity extends Activity {
    private final static String KEY_FIRST_BOOT_DONE = "FIRST_BOOT_DONE";

    // TODO Play and pause two menus separated
    private final static int MENU_PLAY_PAUSE = 0;
    private final static int MENU_PREFERENCES = 1;
    private final static int MENU_HELP = 2;

    private final static int MENU_NEW_SCENE = 10;
    private final static int MENU_LOAD_SCENE = 11;
    private final static int MENU_SAVE_SCENE = 12;

    private ImageButton mModeButton;

    private GLSurfaceView mSurface;
    private Scene mScene;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* Request full screen view */
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);
        mSurface = (GLSurfaceView) findViewById(R.id.gl_surface);

        mScene = new Scene(this);
        mSurface.setRenderer(mScene.getSceneRenderer());

        mScene.setSceneMode(Scene.SCENE_MODE_DRAWING);

        mModeButton = (ImageButton) findViewById(R.id.option_button);
        mModeButton.setImageResource(android.R.drawable.ic_menu_edit);
        mModeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                changeSceneMode();
            }
        });

        final ZoomControls zoomControl = (ZoomControls) findViewById(R.id.zoom_control);
        zoomControl.setOnZoomInClickListener(new OnClickListener() {
            public void onClick(View v) {
                mScene.onZoom(1.2f);
            }
        });

        zoomControl.setOnZoomOutClickListener(new OnClickListener() {
            public void onClick(View v) {
                mScene.onZoom(0.8333f);
            }
        });
    }

    private void changeSceneMode() {
        final CharSequence[] items = { getText(R.string.mode_view),
                getText(R.string.mode_drawing) };

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_scene_mode);

        builder.setSingleChoiceItems(items,
                mScene.getSceneMode() == Scene.SCENE_MODE_PREVIEW ? 0 : 1,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                        case 0:
                            mModeButton
                                    .setImageResource(android.R.drawable.ic_menu_mylocation);
                            mScene.setSceneMode(Scene.SCENE_MODE_PREVIEW);
                            break;
                        case 1:
                            mModeButton
                                    .setImageResource(android.R.drawable.ic_menu_edit);
                            mScene.setSceneMode(Scene.SCENE_MODE_DRAWING);
                            break;
                        }
                        dialog.dismiss();
                    }

                });
        final AlertDialog chioceDialog = builder.create();
        chioceDialog.show();
    }

    /**
     * Creates the menu items
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_PLAY_PAUSE, 0, R.string.simulate).setIcon(
                this.getResources().getDrawable(R.drawable.ic_menu_flash));
        menu.add(1, MENU_PREFERENCES, 1, R.string.preferences).setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.add(1, MENU_HELP, 3, R.string.help).setIcon(
                android.R.drawable.ic_menu_help);
        menu.add(10, MENU_NEW_SCENE, 4, R.string.new_scene).setIcon(
                this.getResources().getDrawable(R.drawable.ic_menu_globe));
        menu.add(10, MENU_LOAD_SCENE, 5, R.string.load_scene);
        menu.add(10, MENU_SAVE_SCENE, 6, R.string.save_scene).setIcon(
                android.R.drawable.ic_menu_save);

        return true;
    }

    /**
     * Handles item selections
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_PLAY_PAUSE:
            if (mScene.getScenePhysics().isSimulating())
                mScene.getScenePhysics().stopSimulation();
            else
                mScene.getScenePhysics().startSimulation();
            return true;
        case MENU_PREFERENCES:
            return true;
        case MENU_HELP:
            showHelp();
            return true;
        case MENU_NEW_SCENE:
            mSurface.onPause();
            mScene.getScenePhysics().stopSimulation();
            mScene.clearScene();
            System.gc();
            mSurface.onResume();
            return true;
        case MENU_LOAD_SCENE:
            loadScene();
            return true;
        case MENU_SAVE_SCENE:
            saveScene();
            return true;
        }
        return false;
    }

    private boolean loadScene() {
        final File infile = new File(Environment.getExternalStorageDirectory(),
                "flingbox/scene.xml");
        final FileReader inFileReader;
        boolean readSuccess = false;
        mScene.clearScene();
        mScene.getScenePhysics().stopSimulation();
        try {
            if (!infile.exists())
                return false;
            inFileReader = new FileReader(infile);
            readSuccess = XmlImporter.importXml(inFileReader, mScene
                    .getParser());
            inFileReader.close();
        } catch (Exception e) {
            Log.e("flingbox", "Error loading scene: " + e);
            e.printStackTrace();
            readSuccess = false;
        }
        if (readSuccess)
            Toast.makeText(this, R.string.scene_loaded, Toast.LENGTH_SHORT)
                    .show();
        else
            Toast.makeText(this, R.string.scene_load_error, Toast.LENGTH_SHORT)
                    .show();
        return readSuccess;
    }

    /**
     * Exports scene to default output XML
     * 
     * @return
     */
    private boolean saveScene() {
        final File outfile = new File(
                Environment.getExternalStorageDirectory(), "flingbox/scene.xml");
        final FileWriter outFileWriter;
        boolean writeSuccess = false;
        try {
            if (outfile.exists())
                outfile.delete(); // Clear last saved file
            outfile.createNewFile(); // Create new file
            outFileWriter = new FileWriter(outfile);
            writeSuccess = XmlExporter.exportXml(outFileWriter, mScene
                    .getSerializer());
            outFileWriter.close();
        } catch (Exception e) {
            Log.e("flingbox", "Error saving scene: " + e);
            e.printStackTrace();
            writeSuccess = false;
        }
        if (writeSuccess)
            Toast.makeText(this, R.string.scene_saved, Toast.LENGTH_SHORT)
                    .show();
        else
            Toast.makeText(this, R.string.scene_save_error, Toast.LENGTH_SHORT)
                    .show();
        return writeSuccess;
    }

    /**
     * Called when activity Pause
     */
    @Override
    public void onPause() {
        super.onPause();
        mScene.getScenePhysics().stopSimulation();
        mSurface.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSurface.onResume();
    }

    /**
     * Called when activity Stops
     */
    public void onStop() {
        super.onStop();
        mScene.getScenePhysics().stopSimulation();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        showHelp();
        if (savedInstanceState == null)
            showHelp();
        else if (!savedInstanceState.containsKey(KEY_FIRST_BOOT_DONE))
            showHelp();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Preferences.onSavePreferences(outState);
    }

    /**
     * Called only when orientation changed. This is called because the
     * android:configChanges="orientation" in AndroidManifest.xml
     */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Called when touch event occurs.
     */
    public boolean onTouchEvent(MotionEvent ev) {
        return mScene.onTouchEvent(ev);
    }

    /**
     * Called when trackball scroll event occurs
     */
    public boolean onTrackballEvent(MotionEvent ev) {
        return mScene.onTrackballEvent(ev);
    }

    /**
     * Show help dialog
     */
    private void showHelp() {
        final Dialog helpDialog = new Dialog(this);
        helpDialog.setTitle(R.string.help);
        helpDialog.setContentView(R.layout.help);
        helpDialog.show();

        Button aboutButton = (Button) helpDialog
                .findViewById(R.id.about_button);
        if (aboutButton == null)
            return;

        aboutButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                helpDialog.dismiss();
                showAboutDialog();
            }
        });

    }

    /**
     * Show about dialog
     */
    private void showAboutDialog() {
        Dialog aboutDialog = new Dialog(this);
        aboutDialog.setTitle(R.string.help_about);
        aboutDialog.setContentView(R.layout.about_dialog);
        aboutDialog.show();
    }

}