set -e

for P in toools java4unix jdotgen jExperiment jaseto; do
    echo '***' $P

    # Supprime les dossiers si ils existent déjà
    if [ -d $P ]; then
        rm -rf $P
    fi

    git clone https://github.com/lhogie/$P.git
    cd "$P"
    mvn clean install -Dgpg.skip=true
    cd ..
done

mv jdotgen/src/jdotgen src/main/java/jdotgen

read
