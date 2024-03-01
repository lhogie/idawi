MSG="for Idawi: "$*

for P in jdotgen jExperiment toools jaseto java4unix
do
	echo '***' $P
		git -C ../$P/ add .
		git -C ../$P/ commit -m "$MSG"
		git -C ../$P/ push
done
	