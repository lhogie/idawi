MSG=$*

if [ -z "-$MSG" ]
then
	read -p "Enter commit message: " MSG
fi
		
git commit -a -m "$MSG" && git push