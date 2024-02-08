# Project Title

TwitchVOD : An Android TV Application to view Twitch VOD without sub.
Inspired by the Leanback example from Google in Kotlin, and modified it for Twitch VOD no Sub.


## Description

Some streamers require sub on Twitch to replay a previous stream.
This application allows to see your favourite streamer VODs without subbing.
The method is inspired by the method of [besuper/TwitchNoSub](https://github.com/besuper/TwitchNoSub). All credits to it.
Although it can be used on Android phones, it's buggy. So please don't use it on phones.
For PC : An extension is available for Chrome, Firefox ([here](https://github.com/besuper/TwitchNoSub/releases))
For Android phones : Please use Kiwi Browser and use [this extension](https://github.com/meta11ica/mTwitchNoSub-also/releases)
For Android TV : you are in the good place. Download the [APK here](https://github.com/meta11ica/TwitchVOD/releases)

## Getting Started

### Notes
I implemented the add to favourites in an ugly way (minimalist) until I work on the Personal Settings Fragment.
#### Add a streamer to your favourite list
Click the Search button, then type your streamer id (username) and click Search.
The streamer is added to your favorite list.
Hit back and the application will restart with the new list.

#### Remove a streamer to your favourite list
Same thing, click the Search button then type your streamer id (username) and validate (Enter)
If the username exists in your favorite list, he/she will be removed.
Hit back and the application will restart with the new list.


### Installing from releases
You need a File Explorer in your Android TV (for example ES File Explorer). Install it from Play Store.
Please go to [releases section](https://github.com/meta11ica/TwitchVOD/releases) and download the latest APK version and save it to a thumb drive (flash disk).
In Android TV, go to Setting > Security and Check the option "Unknown sources". (please revert it to Unchecked after installation)
Insert the thumb drive and browse it with ES File Explorer.
Install it, you may need to select "Install anyway" (the package is signed by my own keys which are unknown to Google)

### From Source

#### Dependencies

* All dependencies are specified in build.gradle.kts
  
#### Customize and/or Build 

* Clone or download ZIP and open the project in Android Studio
* Build debug
* Modify the code if you wish to contribute (your contribution is welcome)
* build release

### TODO

* Implement the add/remove from Favorite Streamers list in Personal Settings
* Fix bugs of Lives, but as I said better use the Twitch app for Live Streams
* Beautify streamer names and not rely on username only
* Add auto-update in case new release (nice to have)
* ...
```
code blocks for commands
```

## Help

Feel free to open an issue in case of a problem.
```
command to run if program contains helper info
```

## Authors

Contributors names and contact info

## Version History

* 1.0
    * Initial Release

## License

This project is licensed under the [MIT] License - see the LICENSE.md file for details

## Acknowledgments

Inspiration, code snippets, etc.

* [besuper](https://github.com/besuper/TwitchNoSub)
