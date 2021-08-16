# Discover Killer

![Discover Killer logo](https://i.imgur.com/cqUMMNl.png "DiscoverKiller logo")

Discover Killer is an Xposed module that allows you to replace the Google Discover page of your home screen with whatever you want. Tired of garbage clickbait articles and ads? This module can replace the page with your favourite app, or the Google Assistant 'Snapshot' screen - with actual useful and convenient information, like Google Now used to be.

## Examples
![Replacing with the Google Assistant Snapshot page](https://i.imgur.com/JyfqxdE.gif "Replacing with the Google Assistant Snapshot page")
![Replacing with the BBC news app](https://i.imgur.com/LJVzqPO.gif "Replacing with the BBC news app")

(Replacing the page with Assistant 'Updates', and replacing it to launch the BBC News app)

## Configuration:
![Configuration page](https://i.imgur.com/qI3qAV2l.png "Configuration page")

Discover Killer has a number of options to configure the 'overlay' (the panel to the left of your home screen), including:

**When Embedding Google Assistant 'Snapshot'**

- Reload Snapshot when you go home, to always be up-to-date
- Use Material You (Monet) colors, for the background of Snapshot
- Pick a color from your wallpaper to use for Material You

OR

**Launching an app when fully open (any app is supported)**

- Whether to launch the app fresh from each swipe, or to resume the app
- Pick from a list of backgrounds (based on the app's splash screen and icon) to show on the overlay when opened
- Pick a color from your wallpaper to use for Material You

## Compatibility:
The module should work on any launcher that has the Discover page. It hooks the Google app rather than the launcher, which allows this. I've tested it on Lawnchair, Nova Launcher and Action Launcher successfully.

This module has been tested and is working with LSposed, but should also work with EdXposed. Make sure you enable hooking for the Google App when enabling the module (it should be pre-selected)

Downloads are on releases page or on the [XDA thread](https://forum.xda-developers.com/xposed/modules/app-discoverkiller-replace-google-t4120997)
