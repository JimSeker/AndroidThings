package edu.cs4730.robocarbtcontroller;

/**
 * Created by antonio on 2/25/17.
 */

public interface Nes30Listener {

  void onKeyPress(@Nes30Manager.ButtonCode int keyCode, boolean isDown);

}
