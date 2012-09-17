# The Mars Ogler

Scrape all metadata regarding publicly released Mars Science Laboratory
(a.k.a. Curiosity) photographs as clojure maps, with stupid-simple
filesystem caching.

## Usage

Requires [leiningen][lein]. Scrape by running `lein run -m mars-ogler.scrape` in
the project root. Some stuff about dependencies will scroll by, which can
probably be ignored. Once the scraper is going, it will print stuff about its
progress to stdout. It ratelimits itself to no more than one page every 500
milliseconds, so it will take a few minutes. Once all new images have been
fetched, the new and total image counts are printed to stdout.  After finishing,
the data (as readable clojure forms) are cached in `cache/images`, and the last
seen id (i.e., the exclusive stop-point for the next scan) is cached in
`cache/last-id`.  Subsequent runs will only fetch new images (but the cache will
still contain all the images).


[lein]: http://leiningen.org

## License

Copyright © 2012 Dan Lidral-Porter

Distributed under the Eclipse Public License, the same as Clojure.
