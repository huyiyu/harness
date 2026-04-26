.PHONY: preview build clean check-hugo

HUGO_DIR = docs

check-hugo:
	@hugo version | grep -q extended || (echo '需要 Hugo extended 版本'; exit 1)

preview: check-hugo
	@git submodule update --init --recursive
	@cd $(HUGO_DIR) && hugo server -D --navigateToChanged --gc

build: check-hugo
	@cd $(HUGO_DIR) && hugo --minify --gc

clean:
	@rm -rf $(HUGO_DIR)/public $(HUGO_DIR)/resources/_gen
