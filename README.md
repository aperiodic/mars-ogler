# The Mars Ogler

Scrape all metadata regarding publicly released Mars Science Laboratory
(a.k.a. Curiosity) photographs as clojure maps, with stupid-simple
filesystem caching.

## Usage

Requires [leiningen][lein]. Start an endless scraper by running `lein run -m
mars-ogler.scrape` in the project root. Some stuff about dependencies will
scroll by, which can probably be ignored. Once the scraper is going, it will
print information about its progress to stdout. It ratelimits itself by waiting
for half a second before making a new request, so it will take a few minutes.
Once all new images have been fetched, the new and total image counts are
printed, and the new list of all images is serialized to disk as readable forms
in `cache/images`. Until it's terminated, the ogler will check for new images
every minute or so, update its state if it finds any, and print a report to
stdout.

[lein]: http://leiningen.org

## License

Copyright © 2012 Dan Lidral-Porter

Distributed under the Eclipse Public License, the same as Clojure.
