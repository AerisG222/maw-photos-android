# Parity with the web app

Written 2026-09-18 from a code comparison between this app and `maw-photos-solid` (branch
`reimagine-ui`).
It was based on reading the code of both apps; nothing here has been checked in a running build.

Items 1-4 were implemented on 2026-09-18 and are recorded below as done, with what was decided where
the
item left a decision open. Nothing here has been exercised on a device: the unit tests, ktlint and
Android
lint all pass, and the migration was checked against Room's exported schema, but no build has been
run.

Both apps cover the same browsing areas: categories by year, people and clans, places, random, and
search.
Person, place and clan feeds offer the same controls in both: shuffle, favourites only, show
categories.

## 1. The share button is announced as "Toggle Slideshow" — done

`ui/components/mediapager/ButtonBar.kt` now uses `share_photo_icon_description` for the share
button's
`contentDescription`.

## 2. Who and Where in the details sheet — done

`DetailTabs` now offers four tabs: Comments, EXIF, Who and Where. Each is still read only when its
own tab
is reached, so opening the sheet costs one call rather than four.

**Who (photos only).** `MediaFaceService` resolves the detected faces against the people list and
answers
with one `MediaFaces` value - the boxes to draw, the people they belong to, and how many faces
nobody could
be put to. The face overlay and the Who card both read that one value, so the two cannot disagree;
that is
what `src/_media/faces/_peopleInFaces.ts` does on the web side. Each person is listed once with
their face
crop and leads to `MediaFeedSubject.Person`; the leftovers are counted at the foot rather than left
out.
The tab is offered only when face access is not `ScopeAccess.Denied`, the rule `ButtonBar` already
used,
and never for a video.

Two things had to change underneath. The overlay used to be the only thing that fetched faces, and
only
while the preference was on; the Who card needs them either way, so `MediaListAction.FetchFaces`
fetches
on the card's behalf and `MediaListState.facesToHighlight` gates the *drawing* on the preference
instead of
the fetching. And faces are now null until something has asked, so the card can wait rather than say
nobody
is here while it is still asking.

**Where (all media).** `GET media/{mediaId}/places` is new on `PlaceApi`, with a client method, a
`PlaceRepository.getMediaPlaces` cached by media id the way faces are, and a `MediaPlaceService`
holding the
answer for the active item. Sorted broadest first by the place's kind (`broadestFirst` in
`domain/models/Place.kt`), each row showing the kind and the media count and leading to
`navigateToPlace`.
A media item that was never placed says so rather than showing an empty tab.

## 3. Videos can be favourited — done; share stays photo-only

Favourite moved outside `ButtonBar`'s photo-only block. Share did not: it would mean downloading the
video
file and sharing it with its real MIME type, which `ui/shared/ShareHelper.kt` is not built for, and
the doc
already allowed leaving it. Rotate, face highlights and the slideshow stay photo-only.

## 4. Settings: follow the web app's simplifications — done, all three removed

**Decided: thumbnail size goes.** The grid is `GridCells.Adaptive`, so the setting was never about
how much
fits on screen - it moved the minimum column width between 90, 120 and 180dp, and on a phone that is
three
or four across either way. One constant, `MEDIA_GRID_ITEM_SIZE` in
`ui/components/mediagrid/MediaGrid.kt`,
replaces it at the old medium default of 120dp. **The badge toggles go too**: the favourite and
media-type
badges are always drawn.

That removed five copies of the thumbnail setting and four of each badge switch, which were the bulk
of the
settings screen. It touched the five preference models, their Room entities, the repositories, and
every
grid that read them. `MIGRATION_21_22` rebuilds the five preference tables rather than dropping
columns -
`ALTER TABLE ... DROP COLUMN` needs SQLite 3.35 and the version on the device is whatever that
Android
release bundled.

**Display Type** (grid or list) stays for categories and search, as the doc said it should.

## 5. Later, if wanted

Not needed for consistency; listed so they are not forgotten.

- **Category map view.** The web app can show a category's photos as markers on a map. Nothing
  similar
  exists here.
- **Download.** The web app can download a photo or a whole category. Share mostly covers this on a
  phone.
- **Sharing a video.** See item 3 - it needs the file downloaded and shared with its real MIME type.

## Not gaps

These differences are deliberate and should stay:

- Android only: upload, the random-photo widget, new-category notifications.
- Web only: keyboard shortcuts, Stats, the Adjust, Histogram and Minimap cards, and the admin tools
  (metadata editing, bulk edit, place covers). These are desktop review tools.
- Places are walked as a tree here rather than searched. That was already decided (see the comment
  in
  `ui/screens/places/PlacesScreen.kt`).

## Going the other way

For reference, three changes were suggested for the **web** app to match this one. Two are done (
2026-09-18):
recent search history (kept on each device, offering the same 5/10/20/30/50 choices as here), and
Random
loading only on request, though a playing slideshow still loads more as it nears the end. Still
open: a
"12 / 240" position and the year/category origin on an open photo.
