#!/bin/sh
sed -i -b s/"db-mf2fp8rvt8wvvbd"/"db-$1"/ AndroidManifest.xml
sed -i -b s/"mf2fp8rvt8wvvbd"/"$1"/ src/ru/orangesoftware/financisto/export/dropbox/Dropbox.java
sed -i -b s/"jru0v69j9krfnw1"/"$2"/ src/ru/orangesoftware/financisto/export/dropbox/Dropbox.java
echo "Done, don't forget to call bzr revert after building apk"