var exec = require("cordova/exec");

module.exports = {
    checkStatus: function (port, callback) {
        exec(function (result) {
                callback(null, result)
            },
            function (error) {
                callback(error)
            }, 'StarIOPlugin', 'checkStatus', [port]);
    },
    portDiscovery: function (type, callback) {
        type = type || 'All';
        exec(function (result) {
                callback(null, result)
            },
            function (error) {
                callback(error)
            }, 'StarIOPlugin', 'portDiscovery', [type]);
    },
    openCashDrawer: function (port, callback) {
        exec(function (result) {
                callback(null, result)
            },
            function (error) {
                callback(error)
            }, 'StarIOPlugin', 'openCashDrawer', [port]);
    },
    connect: function (port, callback) {
        var connected = false;
        exec(function (result) {
                //On initial connection - fire callback, otherwise fire a window event
                if (!connected) {
                    callback(null, result);
                    connected = true;
                } else {
                    //This event will be to notify of events like barcode scans
                    cordova.fireWindowEvent("starIOPluginData", result);
                }
            },
            function (error) {
                callback(error)
            }, 'StarIOPlugin', 'connect', [port]);
    },
    builder: function (options) {
        return new Builder(options);
    }
};

function Builder(options){
    if(!options) options = {};
  
    this.paperWidth = options.width || 384;
    this.commands = [];
  
    function error(str){
      throw new Error(str);
    }
  
    this.text = function(input, style){
      var _style     = style          || {};
      _style.size    = _style.size    || 15;
      _style.color   = _style.color   || 'black';
      _style.font    = _style.font    || 'default';
      _style.weight  = _style.weight  || 'normal';
      _style.align   = _style.align   || 'normal';
      _style.bgcolor = _style.bgcolor || 'white';
  
      this.commands.push({
        type: 'text',
        text: input,
        style: _style
      });
  
      return this;
    };

    this.image = function(input, style){
      var _style     = style          || {};
      _style.width   = _style.width   || 325;
      _style.align   = _style.align   || 'center';

      this.commands.push({
        type: 'image',
        image: input,
        width: _style.width,
        align: _style.align
      });

      return this;
    }
  
    this.cutPaper = function(){
      this.commands.push({ type: 'cutpaper' });
      return this;
    }
  
    this.print = function(port, callback){
        var args = [{
            paperWidth: this.paperWidth,
            port: port,
            commands: this.commands
        }];

        exec(function (result) {
            callback(null, result)
        },
        function (error) {
            callback(error)
        }, 'StarIOPlugin', 'printReceipt', args);
    }
  }