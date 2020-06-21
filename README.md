# DiscoverKiller

![DiscoverKiller logo](https://i.imgur.com/cqUMMNl.png "DiscoverKiller logo")

DiscoverKiller is an Xposed module that allows you to replace the Google Discover page of your home screen with whatever you want. Tired of garbage clickbait articles and ads? This module can replace the page with your favourite app, or the Google Assistant 'Updates' screen - with actual useful and convenient information, like Google Now used to be.

## Examples
![Replacing with the Google Assistant updates page](https://i.imgur.com/HE1yG75.gif "Replacing with the Google Assistant updates page")
![Replacing with the BBC news app](https://i.imgur.com/CdrrK5F.gif "Replacing with the BBC news app")

(Replacing the page with Assistant 'Updates', and replacing it to launch the BBC News app)

## Configuration:
![Configuration page](https://i.imgur.com/MRbYd5Pl.png "Configuration page")

DiscoverKiller has three options for the left pane behaviour: Open the Assistant 'Updates' screen, choose a custom app to launch (any of your apps will work) or use the standard behaviour (if you want to temporarily go back without having to reboot). If you're using the 'Updates' screen behaviour, there's also an option to close the app by swiping right (swiping left, although integrated, doesn't work properly due to the gestures already on that screen).

## Compatibility:
The module should work on any launcher that has the Discover page. It hooks the Google app rather than the launcher, which allows this. I've tested it on Lawnchair, Nova Launcher and Action Launcher successfully.
The module is designed to work with EdXposed, and has not been submitted for TaiChi's verification which is required for modules to work with that mod. If there is sufficient demand, I may consider submitting it. EdXposed currently passes Safety Net on a correctly configured device, so there's not a reason to use TaiChi over EdXposed at the moment.

Downloads are on releases page or on the [XDA thread](https://forum.xda-developers.com/xposed/modules/app-discoverkiller-replace-google-t4120997)
