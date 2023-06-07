/*!
 * jQuery based plugin - Tree View
 * Author : vsvvssrao (https://github.com/vsvvssrao)
 * Demo link : https://vsvvssrao.github.io/TreeView/
 * Date: Sat Oct 26 2019
 */
(function ($) {
  $.fn.tree = function (options) {
    var settings = $.extend({
      data: function () { },
      onDemandData: function () { }
    }, options);
    function GetData() {
      return settings.data();
    }
    var _data = GetData();
    function createTree(data, ulClassName) {
      var parentUl = document.createElement('ul');
      parentUl.className = ulClassName;
      data.forEach(element => {
        var liElement = document.createElement('li');
        liElement.setAttribute('data-hasChild', element.hasChild);
        liElement.setAttribute('data-id', element.id);
        liElement.setAttribute('data-isLoaded', false);
        if (element.hasChild) {
          var spanElement = document.createElement('span');
          spanElement.innerHTML = element.displayName;
          spanElement.className = 'tv-caret';
          liElement.append(spanElement);
        } else {
          liElement.innerHTML = element.displayName;
        }
        parentUl.append(liElement);
      });
      return parentUl;
    }
    this.append(createTree(_data, 'tv-ul'));
    $(this).off('click','.tv-caret').on('click','.tv-caret', function () {
      var $this = $(this);
      if (!$this.parent('li').data("isloaded")) {
        // fetch data
        var a = createTree(settings.onDemandData(), 'tv-nested');
        // Append the data
        $this.parent('li').append(a);
        // Set isloaded to true
        $this.parent('li').data("isloaded",true);
      }
      $this.parent('li').find('.tv-nested').toggleClass("active");
      $this.toggleClass("tv-caret-down");

    })
    return this;
  };
}(jQuery));