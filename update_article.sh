set -e
PDF=~/Downloads/downloads/idawi.pdf

if [ -f $PDF ]
then
	ssh hogie@bastion.i3s.unice.fr rm -f public_html/idawi.pdf
	scp -p $PDF hogie@bastion.i3s.unice.fr:public_html/idawi.pdf
	rm -rf $PDF
else
	echo "no new version found!"
fi

