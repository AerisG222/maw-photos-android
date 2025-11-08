# Release Notes

## v9.1 : 11/08/2025
- avoid scenario where swiping back leaves you stuck on a loading screen
- improve styling of login screen
- update dependencies

## v9.0 : 10/13/2025
- major update to account for changes on the backend
- categories can now contain both photos and videos
- all photos and videos re-encoded in avif and av1 for better quality at smaller sizes
- better support for loading appropriately sized media based on device resolution
- update to use Auth0 for authentication
- update dependencies

## v8.6 : 05/31/2025
- try to properly handle edge 2 edge
- code cleanup and updates after playing with gemini 
- attempt to define better baseline profile to improve performance
- update dependencies

## v8.5 : 04/25/2025
- update dependencies

## v8.4 : 12/31/2024
- theme/color related updates
- overhauled light theme for improved contrast
- many dependencies updated

## v8.3.1 : 09/13/2024
- dependency updates

## v8.3 : 08/30/2024
- library updates
- this will definitely fix the authorization issue for good - i hope =D

## v8.2 : 08/25/2024
- library updates
- another attempt to fix the broken auth refresh

## v8.1 : 08/08/2024
- Update libraries and hopefully addresses sizing issue when playing videos in the pager
- Simplify / improve authorization code to avoid errors when updating categories in background

## v8.0 : 07/07/2024
- New support for playing videos that were previously only available on the web!
- Added support for older devices
- Tweaked the theme - dark theme is now more consistent with the "Dusk" theme on the web
- Tons of code updates:
  - Rewrote UI code using Jetpack Compose
  - Upgraded to Kotlin 2.0
  - Changed / Updated almost all libraries
  - Should serve as a strong foundation for future efforts
- Hopefully things work well for you, if not - let me know!

## v7.4 : 11/20/2023
- Fix crash when navigating to search screen
- Migrate build scripts from groovy to kotlin

## v7.3 : 10/18/2023
- Add swipe to refresh gesture to try and load any new categories
- Update libraries

## v7.2 : 09/02/2023
- Update libraries
- Request notification permission on newer versions of Android

## v7.1 : 02/19/2023
- Update libraries
- Increase size of nav buttons and text to reduce touch errors

## v7.0 : 12/31/2021
- Major update rewritten in Kotlin
- Improved UI to be more consistent with photos web app
- Added new search feature
- Support both light and dark themes
- Use new / updated dependencies

## v6.2 : 11/11/2019
- sdk + library updates

## v6.1 : 04/30/2019
- improved logging

## v6.0 : 09/21/2018
- introduce ability to receive photos and videos shared from other apps, then upload these to mikeandwan.us

## v5.7 : 07/03/2018
- remove permissions that are no longer necessary
- improved launch and status icons
- update app name to mirror what is displayed on Google Play
- simplify settings and always check for new categories (every 4hrs)

## v5.6 : 06/26/2018
- try to improve notifications on newer android versions

## v5.5 : 06/19/2018
- another attempt to get refresh tokens working properly

## v5.4 : 06/16/2018
- migrate from poller to jobservice

## v5.3 : 06/12/2018
- fix a lame error introduced in the last fix >=|

## v5.2 : 06/09/2018
- try to fix a threading issue when refreshing the access token
- provide way to force re-authentication

## v5.1 : 06/08/2018
- go immediately to mode screen if already logged in (improved experience when there is poor network condition)

## v5.0 : 06/03/2018
- authentication overhaul to coincide with new website
- update dependencies

## v4.3 : 06/30/2017
- update encryption approach to work on newer android devices

## v4.2 : 06/14/2017
- improve grid image quality

## v4.1 : 06/13/2017
- clean temp files on start
- provide option to wipe cache

## v4.0 : 02/25/2017
- rewrite / re-imagining of the app design
- fix crash on rotate on the photo list screen
- add more customization options to the photo list screen
- using jack+jill w/ java 8
- leverage OSS libraries:
    - butterknife
    - dagger 2
    - okhttp
    - retrofit (w/ jackson)
    - photoview
    - rxjava 2 / rxandroid 2
- remove complicated threading code
- use constraint layouts
- use vector graphics where possible
- removed use of fragments
- optimize some code paths (ex. initialization is fast now)

## v3.2 : 09/22/2016
- fix issue with category thumbnails

## v3.1 : 09/19/2016
- updates based on website changes
- fix an issue in random mode
- minor code cleanup / fixes

## v3.0 : 11/20/2015
- us.mikeandwan.photos.ui updates to better align with material design
- update category list page to show larger thumbnails
- add new category list view to show only thumbnails
- improve photo screen to show image on larger surface, and to fade out toolbars
- improved tablet support
- store photos in a private folder to avoid polluting other apps like Gallery - photos also deleted on uninstall
- delete photos stored in the old location (restart device to clear Gallery cache)
- update code to use picasso
- update code to use android annotations

## v2.2
- now use HTTPS when connecting to services
- internal code naming changes

## v2.1
- support for zoom / pan, and now animates the rotation
- now can view set of random images
- new interaction when flinging between images
- optionally hide the toolbar and/or thumbnail list to allow for larger main image
- restructured code to leverage fragments, resulting in much cleaner code/organization
- minor performance optimization for list views
- support older devices (now support sdk9+)

## v2.0
- improved performance through use of priority queued parallel background tasks
- added support for older devices thanks to android support libraries
- improved handling of password change related issues
- reduced the processing required when first using application
- new setting to control slideshow interval
- main image prefetch

## v1.6
- notification improvements

## v1.5
- rotate images
- add comments
- rate picture
- view exif data
- slideshow mode
- notify when network is unavailable
- try to seamlessly handle session timeouts
- improve notifications, hopefully now you are sent into the app

## v1.4
- store images in a directory that is clearly associated with this application

## v1.3
- fixed crash due to build process stripping details required by jackson parsing library

## v1.2
- updates to support older runtimes

## v1.1
- improve the progress indicators
- add ability to swipe main image to navigate through pictures

## v1.0 : 11/05/2013
First release!  Please let me know if you run into any problems - thanks!
