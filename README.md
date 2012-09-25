# The Mars Ogler

A Curiosity Mars Science Laboratory raw images viewer. [Check out the live
site](http://mars-ogler.com).

## Usage

Requires [leiningen][lein]. Start the ogler with `lein run`, which loads all
cached images, starts a jetty server on port 2001, and enters an endless scrape
loop. On the first run, there will be no cached images, so it'll have to fetch
all the current ones, which will take several minutes. Information about the
scraping progress will be printed to stdout. Once it's finished, the list of
images is serialized to disk, and the total image count will be printed to
stdout. At this point, you can navigate to `localhost:2001` and see the images.
In the scrape loop, the ogler will check for new images every minute or so,
update its states so that the new images will be visible after a page refresh
(if any are found), and print a report to stdout.

[lein]: http://leiningen.org

## License

Copyright © 2012 Dan Lidral-Porter

Distributed under the GNU Affero General Public License (see COPYING).
