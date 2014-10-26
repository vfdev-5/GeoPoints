GeoPoints
=========

This is a 'GeoPoints' android testing application.
The purpose is to be able to create/edit geolocation
points and some additional information about it.

-----
todo
1) OK = update periodically current location on map-page
2) Recode : do not redraw geopoint markers every time MAP_PAGE is activated
    => WHY NOT TO REFRESH MAP LIKE THIS WITHOUT THINKING ABOUT DB/MAP POINTS CORRESPONDENCE
    - draw once and store markers (or just number of drawn points)
3) crash on screen-rotation when photo is taken