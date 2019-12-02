
# This is '.' by the document build stuff

BASE=..
BUILD=$BASE/build

SCRIPTS=$BASE/scripts
HTDOCS=$BASE/htdocs
UNFILTERED=$BASE/vimhelp
VIM_CSS=$BASE/css
VIS_BLOCK_HELP=$BASE/vis-block-help

FILTERED=$BUILD/filtered

VIMHELP_OUT=$BUILD/txt_html
HTDOCS_OUT=$BUILD/htdocs
UC_OUT=$BUILD/update-center

UC_MIRROR=/z/jvi/frs/jVi-for-NetBeans

# only for dirs, ln -s EXIST_DIR LINK_DIR, WIN7 permission issues
# symlink_dir EXIST LINK
symlink_dir() {
    if [[ ! -d "$1" ]]; then
        echo symlink_dir: "$1" is not a directory
        echo "    "symlink_dir EXIST_DIR LINK_DIR
        return 1
    fi
    if [[ -z "$2" ]]; then
        echo symlink_dir: LINK_DIR not specified
        echo "    "symlink_dir EXIST_DIR LINK_DIR
        return 1
    fi
    if [[ -e "$2" ]]; then
        echo symlink_dir: "$2" already exists
        echo "    "symlink_dir EXIST_DIR LINK_DIR
        return 1
    fi
    #ln -s "$1" "$2"
    junction $(cygpath -w "$2") $(cygpath -w "$1")
}

set -e
