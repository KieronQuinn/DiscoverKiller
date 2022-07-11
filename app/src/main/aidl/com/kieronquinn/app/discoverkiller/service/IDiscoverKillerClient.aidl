package com.kieronquinn.app.discoverkiller.service;

//Client interface for settings, sending a special intent to the service with the module working will return this
interface IDiscoverKillerClient {

    boolean areHooksWorking();

}