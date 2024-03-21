set -e

for P in toools java4unix jdotgen jExperiment jaseto 
do
	echo '***' $P
	git clone https://github.com/lhogie/$P.git
	pushd .
	mvn install
	popd
done
	