#!/bin/sh

#If an argument is passed, it will be used as the version number
SUFFIX=${1:+"_$1"}
if [ -z "%2" ]; then
    echo "Only one argument is supported"
    exit 1
fi

# Make directories
mkdir -p bin/vtr
mkdir -p bin/wrapper

# Copy static files
cp src/*.otf bin/vtr
cp saved_dbase.txt bin
cp saved_config.txt bin
cp TransRot.jar bin
cp README.md bin

# Compile source
cd src
javac -d ../bin/vtr Main.java
javac -d ../bin/wrapper Wrapper.java

## Create jar files
cd ../bin
jar cfe vtr.jar Main -C vtr/ .
jar cfe wrapper.jar Wrapper -C wrapper/ .

## Package binaries
chmod u+x vtr.jar
chmod u+x TransRot.jar

zip vtr${SUFFIX}-win.zip README.md saved_config.txt saved_dbase.txt TransRot.jar vtr.jar wrapper.jar
zip vtr${SUFFIX}-mac.zip README.md saved_config.txt saved_dbase.txt TransRot.jar vtr.jar
zip vtr${SUFFIX}-linux.zip README.md saved_config.txt saved_dbase.txt TransRot.jar vtr.jar