set -e
cd target/classes/
jar cf idawi.jar idawi/
rsync -a idawi.jar srv-kairos.inria.fr:/tmp/
rm -f idawi.jar
echo 'rsync -a srv-kairos.inria.fr:/tmp/idawi.jar .'
