class InformationNode {

    constructor (jsonNode) {
        this.mydata = jsonNode;
    }

    generateHTMLDescriptorAttribut (valeur, label, ID) {
        let htmlValue = null;
        if (typeof valeur == "string" || typeof valeur == "number") {
            htmlValue = $("<div></div>")
                .addClass("col-sm-10")
                .append($("<input>")
                    .attr({
                        type: "text",
                        id: ID,
                        value: valeur
                    })
                    .prop("readonly", true)
                    .addClass("form-control")
                );
        }

        if (typeof valeur == "object") {
            htmlValue = $("<div></div>")
                .addClass("col-sm-10");
            $.each(valeur, (key, value) => {
                htmlValue.append(this.generateHTMLDescriptorAttribut(value, key, "id-" + key));
            });
        }

        if (Array.isArray (valeur)) {
            let select = $("<select>")
                .attr({
                    id: ID,
                })
                .prop("multiple", true)
                .addClass("form-control");
            valeur.forEach ((val) => {
                select.append($("<option></option>")
                    .text(val)
                );
            });

            htmlValue = $("<div></div>")
                .addClass("col-sm-10")
                .append(select);
        }

        return $("<div></div>")
            .addClass ("form-group")
            .addClass ("row")
            .append($("<label></label>")
                .text(label)
                .addClass("col-sm-2")
                .addClass("col-form-label")
            )
            .append(htmlValue);
    }

    generateHTMLDescriptor () {
        let main = $("<form></form>")
            .attr({
                id: "node-descriptor"
            });

        $.each(this.mydata, (key, value) => {
            main.append(
                this.generateHTMLDescriptorAttribut(value, key, "id-" + key)
            )
        });

        return $("<div></div>")
            .append (main);
    }
}