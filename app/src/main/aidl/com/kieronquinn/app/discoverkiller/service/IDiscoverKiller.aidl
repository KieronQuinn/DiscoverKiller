package com.kieronquinn.app.discoverkiller.service;

//Google App -> Discover Killer service to allow background restarts
interface IDiscoverKiller {

    oneway void setBypassBackgroundStarts(boolean enabled);
    void killOverlayPackage(String packageName);
    void ping();

}