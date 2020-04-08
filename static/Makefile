# INSTALLATION: pip install sphinx && npm install --global node-sass

ISSO_JS_SRC := $(shell find js/app -type f) \
           $(shell ls js/*.js | grep -vE "(min|dev)") \
	       js/lib/requirejs-jade/jade.js

ISSO_JS_DST := js/embed.min.js js/embed.dev.js \
	       js/count.min.js js/count.dev.js

ISSO_CSS := css/isso.css

RJS = r.js

SASS = node-sass

all: js

init:
	(cd js; bower --allow-root install almond requirejs requirejs-text jade)

js/%.min.js: $(ISSO_JS_SRC) $(ISSO_CSS)
	$(RJS) -o js/build.$*.js out=$@

js/%.dev.js: $(ISSO_JS_SRC) $(ISSO_CSS)
	$(RJS) -o js/build.$*.js optimize="none" out=$@

js: $(ISSO_JS_DST)

clean:
	rm -f $(DOCS_MAN_DST) $(DOCS_CSS_DST) $(ISSO_JS_DST)
	rm -rf $(DOCS_HTML_DST)

.PHONY: clean init js
