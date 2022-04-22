#!/bin/bash

echo === diff catalog ~/bin
diff catalog ~/bin

echo === diff combine_uc ~/bin
diff combine_uc ~/bin

echo === diff jvi_python ~/bin
diff jvi_python ~/bin

echo === diff jvi/combine_uc.py ~/bin/python_stuff/jvi
diff jvi/combine_uc.py ~/bin/python_stuff/jvi

# lint diffs for the most part
echo === diff -r jvi ~/bin/python_stuff/jvi
echo === SKIP: diffs from lint modifications
#diff -r jvi ~/bin/python_stuff/jvi
