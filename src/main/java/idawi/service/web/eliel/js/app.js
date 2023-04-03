
var Treedata = {
    "result": [{
        "id": "l01",
        "displayName": "Beverages",
        "hasChild": true,
        "isLoaded": true,
        "children": [{
            "id": "b01",
            "displayName": "Water",
            "hasChild": false,
            "isLoaded": false
        },
        {
            "id": "b01",
            "displayName": "Coffee",
            "hasChild": false,
            "isLoaded": false
        },
        {
            "id": "b01",
            "displayName": "Tea",
            "hasChild": false,
            "isLoaded": false
        }
        ]
    }, {
        "id": "l02",
        "displayName": "Beverages1",
        "hasChild": true,
        "isLoaded": true,
        "children": [{
            "id": "b01",
            "displayName": "Water",
            "hasChild": false,
            "isLoaded": false
        },
        {
            "id": "b01",
            "displayName": "Coffee",
            "hasChild": false,
            "isLoaded": false
        },
        {
            "id": "b01",
            "displayName": "Tea",
            "hasChild": false,
            "isLoaded": false
        }
        ]
    }, {
        "id": "l03",
        "displayName": "Beverages2",
        "hasChild": true,
        "isLoaded": true,
        "children": [{
            "id": "b01",
            "displayName": "Water",
            "hasChild": false,
            "isLoaded": false
        },
        {
            "id": "b01",
            "displayName": "Coffee",
            "hasChild": false,
            "isLoaded": false
        },
        {
            "id": "b01",
            "displayName": "Tea",
            "hasChild": false,
            "isLoaded": false
        }
        ]
    }, {
        "id": "l05",
        "displayName": "Beverages2",
        "hasChild": false,
        "isLoaded": true,
        "children": []
    }]
};

$('#myListTree').tree({
    data: function(){
        return Treedata.result
    },
    onDemandData: function () {
        return Treedata.result
    }
});


