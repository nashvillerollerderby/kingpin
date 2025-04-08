const _windowFunctions = {
  /* Display area dimensions */
  getAspectDimensions: function (aspect, overflow) {
    var width, height, top, bottom, left, right;
    if (aspect > $(window).width() / $(window).height() === (overflow === true)) {
      width = Math.round(aspect * $(window).height());
      height = $(window).height();
      top = bottom = 0;
      left = right = ($(window).width() - width) / 2;
    } else {
      width = $(window).width();
      height = Math.round($(window).width() / aspect);
      top = bottom = ($(window).height() - height) / 2;
      left = right = 0;
    }
    return { width: width, height: height, top: top, bottom: bottom, left: left, right: right };
  },
  get4x3Dimensions: function (overflow) {
    return this.getAspectDimensions(4 / 3, overflow);
  },
  get16x9Dimensions: function (overflow) {
    return this.getAspectDimensions(16 / 9, overflow);
  },

  /* URL parameters */
  getParam: function (param) {
    return new URL(window.location).searchParams.get(param);
  },
  hasParam: function (param) {
    return this.getParam(param) != null;
  },
  checkParam: function (param, value) {
    return value === _windowFunctions.getParam(param);
  },

  /* Zoom for mobile views */
  configureZoom: function () {
    if (!_windowFunctions.checkParam('zoomable', 1)) {
      $('head').append('<meta name="viewport" content="width=device-width, user-scalable=no">');
    } else {
      $('head meta[name=viewport]').remove();
    }
  },

  /* DOM element sorting
   *
   * This inserts the provided newChild directly under the parent,
   * using the specified comparator to insert at the correct spot.
   * If startIndex (which is 0-based) is specified, the newChild
   * will be inserted no earlier than that index (unless there
   * are not enough children to reach that index, in which case the
   * newChild will be appended).  The comparator should accept two
   * parameters (both actual DOM elements, not jQuery objects),
   * the first being the existing child to compare, and the second
   * being the newChild that is being inserted, and it should return
   * true to indicate the newChild is "before" the existing child,
   * and false to indicate the newChild is "after" the existing child.
   * This insertion function does not attempt to maintain ordering
   * for "equal" children, and so the comparator does not provide
   * for indicating equality; it can return either true or false
   * for "equal" children, which will be sorted in the order they
   * are inserted.
   */
  appendSorted: function (parent, newChild, comparator, startIndex) {
    var child = null;
    startIndex = startIndex || 0;
    parent.children().each(function (i) {
      if (newChild[0] !== child && startIndex <= i && comparator(this, newChild[0])) {
        child = this;
        return false;
      }
    });
    if (child) {
      newChild.insertBefore(child);
    } else {
      parent.append(newChild);
    }
    return parent;
  },

  /* Comparator functions
   *
   * These are convenience functions to use for various
   * types of sorting.  The "Alpha" sorting does an
   * alphabetical sorting using a "greater than" comparison.
   * The "AlphaNum" sorting puts all pure numbers first
   * (using the "Alpha" sort but with the params converted to
   * Numbers) sorted in numerical order, then the rest are given
   * to the "Alpha" sort.
   */
  appendAlphaSorted: function (parent, newChild, startIndex) {
    var comp = _windowFunctions.alphaCompareByNodeName;
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaSortedByAttr: function (parent, newChild, attrName, startIndex) {
    var comp = function (a, b) {
      return _windowFunctions.alphaCompareByAttr(attrName, a, b);
    };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaNumSortedByAttr: function (parent, newChild, attrName, startIndex) {
    var comp = function (a, b) {
      return _windowFunctions.numCompareByAttr(attrName, a, b);
    };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaSortedByProp: function (parent, newChild, propName, startIndex) {
    var comp = function (a, b) {
      return _windowFunctions.alphaCompareByProp(propName, a, b);
    };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaNumSortedByProp: function (parent, newChild, propName, startIndex) {
    var comp = function (a, b) {
      return _windowFunctions.numCompareByProp(propName, a, b);
    };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaSortedByData: function (parent, newChild, dataName, startIndex) {
    var comp = function (a, b) {
      return _windowFunctions.alphaCompareByData(dataName, a, b);
    };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaNumSortedByData: function (parent, newChild, dataName, startIndex) {
    var comp = function (a, b) {
      return _windowFunctions.numCompareByData(dataName, a, b);
    };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaSortedByText: function (parent, newChild, startIndex) {
    var comp = function (a, b) {
      return _windowFunctions.alphaCompareByText(a, b);
    };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },
  appendAlphaNumSortedByText: function (parent, newChild, startIndex) {
    var comp = function (a, b) {
      return _windowFunctions.numCompareByText(a, b);
    };
    return _windowFunctions.appendSorted(parent, newChild, comp, startIndex);
  },

  alphaCompare: function (a, b) {
    return a > b;
  },
  alphaCompareByNodeName: function (a, b) {
    return _windowFunctions.alphaCompare(a.nodeName, b.nodeName);
  },
  alphaCompareByAttr: function (n, a, b) {
    return _windowFunctions.alphaCompare($(a).attr(n), $(b).attr(n));
  },
  alphaCompareByProp: function (n, a, b) {
    return _windowFunctions.alphaCompare($(a).prop(n), $(b).prop(n));
  },
  alphaCompareByData: function (n, a, b) {
    return _windowFunctions.alphaCompare($(a).data(n), $(b).data(n));
  },
  alphaCompareByText: function (a, b) {
    return _windowFunctions.alphaCompare($(a).text(), $(b).text());
  },
  numCompare: function (a, b) {
    var numA = Number(a),
      numB = Number(b);
    if (!isNaN(numA) && !isNaN(numB)) {
      // both numbers
      return _windowFunctions.alphaCompare(numA, numB);
    } else if (isNaN(numA) && isNaN(numB)) {
      // both non-numbers
      return _windowFunctions.alphaCompare(a, b);
    } else {
      // b num, a non-num? a>b (true).  a num, b non-num? a<b (false).
      return isNaN(numA);
    }
  },
  numCompareByAttr: function (n, a, b) {
    return _windowFunctions.numCompare($(a).attr(n), $(b).attr(n));
  },
  numCompareByProp: function (n, a, b) {
    return _windowFunctions.numCompare($(a).prop(n), $(b).prop(n));
  },
  numCompareByData: function (n, a, b) {
    return _windowFunctions.numCompare($(a).data(n), $(b).data(n));
  },
  numCompareByText: function (a, b) {
    return _windowFunctions.numCompare($(a).text(), $(b).text());
  },
};
