package fr.sellsy.cordova;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;


import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;


import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starioextension.StarIoExt.Emulation;
import com.starmicronics.starioextension.ICommandBuilder.CutPaperAction;
import com.starmicronics.starioextension.ICommandBuilder.InitializationType;
import com.starmicronics.starioextension.ICommandBuilder.BarcodeSymbology;
import com.starmicronics.starioextension.ICommandBuilder.BarcodeWidth;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Color;



import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.util.Base64;
import android.graphics.BitmapFactory;


/**
 * This class echoes a string called from JavaScript.
 */
public class StarIOPlugin extends CordovaPlugin {


    private CallbackContext _callbackContext = null;
    String strInterface;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if(_callbackContext == null){
            _callbackContext = callbackContext;
        }

        if (action.equals("checkStatus")) {
            String portName = args.getString(0);
            String portSettings = getPortSettingsOption(portName);
            this.checkStatus(portName, portSettings, callbackContext);
            return true;
        }else if (action.equals("portDiscovery")) {
            String port = args.getString(0);
            this.portDiscovery(port, callbackContext);
            return true;
        }else {
            this.printReceipt(args, callbackContext);
            return true;
        }
    }


    public void checkStatus(String portName, String portSettings, CallbackContext callbackContext) {

        final Context context = this.cordova.getActivity();
        final CallbackContext _callbackContext = callbackContext;

        final String _portName = portName;
        final String _portSettings = portSettings;

        cordova.getThreadPool()
                .execute(new Runnable() {
                    public void run() {

                        StarIOPort port = null;
                        try {

                            port = StarIOPort.getPort(_portName, _portSettings, 10000, context);

                            // A sleep is used to get time for the socket to completely open
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }

                            StarPrinterStatus status;
                            status = port.retreiveStatus();

                            JSONObject json = new JSONObject();
                            try {
                                json.put("offline", status.offline);
                                json.put("coverOpen", status.coverOpen);
                                json.put("cutterError", status.cutterError);
                                json.put("receiptPaperEmpty", status.receiptPaperEmpty);
                            } catch (JSONException ex) {

                            } finally {
                                _callbackContext.success(json);
                            }


                        } catch (StarIOPortException e) {
                            _callbackContext.error("Failed to connect to printer :" + e.getMessage());
                        } finally {

                            if (port != null) {
                                try {
                                    StarIOPort.releasePort(port);
                                } catch (StarIOPortException e) {
                                    _callbackContext.error("Failed to connect to printer" + e.getMessage());
                                }
                            }

                        }


                    }
                });
    }


    private void portDiscovery(String strInterface, CallbackContext callbackContext) {

        JSONArray result = new JSONArray();
        try {

            if (strInterface.equals("LAN")) {
                result = getPortDiscovery("LAN");
            } else if (strInterface.equals("Bluetooth")) {
                result = getPortDiscovery("Bluetooth");
            } else if (strInterface.equals("USB")) {
                result = getPortDiscovery("USB");
            } else {
                result = getPortDiscovery("All");
            }

        } catch (StarIOPortException exception) {
            callbackContext.error(exception.getMessage());

        } catch (JSONException e) {

        } finally {

            Log.d("Discovered ports", result.toString());
            callbackContext.success(result);
        }
    }


    private JSONArray getPortDiscovery(String interfaceName) throws StarIOPortException, JSONException {
        List<PortInfo> BTPortList;
        List<PortInfo> TCPPortList;
        List<PortInfo> USBPortList;

        final Context context = this.cordova.getActivity();
        final ArrayList<PortInfo> arrayDiscovery = new ArrayList<PortInfo>();

        JSONArray arrayPorts = new JSONArray();


        if (interfaceName.equals("Bluetooth") || interfaceName.equals("All")) {
            BTPortList = StarIOPort.searchPrinter("BT:");

            for (PortInfo portInfo : BTPortList) {
                arrayDiscovery.add(portInfo);
            }
        }
        if (interfaceName.equals("LAN") || interfaceName.equals("All")) {
            TCPPortList = StarIOPort.searchPrinter("TCP:");

            for (PortInfo portInfo : TCPPortList) {
                arrayDiscovery.add(portInfo);
            }
        }
        if (interfaceName.equals("USB") || interfaceName.equals("All")) {
            USBPortList = StarIOPort.searchPrinter("USB:", context);

            for (PortInfo portInfo : USBPortList) {
                arrayDiscovery.add(portInfo);
            }
        }

        for (PortInfo discovery : arrayDiscovery) {
            String portName;

            JSONObject port = new JSONObject();
            port.put("name", discovery.getPortName());

            if (!discovery.getMacAddress().equals("")) {

                port.put("macAddress", discovery.getMacAddress());

                if (!discovery.getModelName().equals("")) {
                    port.put("modelName", discovery.getModelName());
                }
            } else if (interfaceName.equals("USB") || interfaceName.equals("All")) {
                if (!discovery.getModelName().equals("")) {
                    port.put("modelName", discovery.getModelName());
                }
                if (!discovery.getUSBSerialNumber().equals(" SN:")) {
                    port.put("USBSerialNumber", discovery.getUSBSerialNumber());
                }
            }

            arrayPorts.put(port);
        }

        return arrayPorts;
    }




    private String getPortSettingsOption(String portName) {
        String portSettings = "";

        if (portName.toUpperCase(Locale.US).startsWith("TCP:")) {
            portSettings += ""; // retry to yes
        } else if (portName.toUpperCase(Locale.US).startsWith("BT:")) {
            portSettings += ";p"; // or ";p"
            portSettings += ";l"; // standard
        }

        return portSettings;
    }


    private boolean printReceipt(JSONArray args, CallbackContext callbackContext) throws JSONException {

        Context context = this.cordova.getActivity();

        JSONObject params = args.getJSONObject(0);
        String portName = params.getString("port");
        String portSettings = getPortSettingsOption(portName);


        return sendCommand(context, portName, portSettings, params, callbackContext);
    }


    public static byte [] getEncodedString(String input){
        return input.getBytes(java.nio.charset.Charset.forName("UTF-8"));
    }

    private static void openCashDrawer(ICommandBuilder builder) {
        builder.appendRaw(new byte[] { 0x07 });
    }

    private static void createImage(ICommandBuilder builder, JSONObject command) throws JSONException {
        String encodedImage = "data:image/gif;base64,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";
                //command.getString("image");

        String align = command.getString("align");

        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);

        BitmapFactory.Options bitmapLoadingOptions = new BitmapFactory.Options();
        bitmapLoadingOptions.inPreferredConfig = Bitmap.Config.RGB_565;

        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length, bitmapLoadingOptions);

        /*
        if (align.equals("right")) {
            builder.appendBitmapWithAlignment(decodedByte, true, ICommandBuilder.AlignmentPosition.Right);
        }
        else if (align.equals("left")) {
            builder.appendBitmapWithAlignment(decodedByte, true, ICommandBuilder.AlignmentPosition.Left);
        } else {
            builder.appendBitmapWithAlignment(decodedByte, true, ICommandBuilder.AlignmentPosition.Center);
        }*/
        builder.appendBitmap(decodedByte, true);
    }

    private static void createText(ICommandBuilder builder, JSONObject command, int width) throws JSONException {
        String textToPrint = command.getString("text");
        JSONObject style = command.getJSONObject("style");

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(false);

        Typeface family = Typeface.DEFAULT;

        String font = style.getString("font");

        if(font.equals("monospace")) {
            family  = Typeface.MONOSPACE;
        }
        else if (font.equals("sans serife")) {
            family  = Typeface.SANS_SERIF;
        }
        else if (font.equals("serife")) {
            family  = Typeface.SERIF;
        }

        int weightInt = Typeface.NORMAL;
        String weight = style.getString("weight");

        if(weight.equals("bold")) {
            weightInt = Typeface.BOLD;
        }
        else if (weight.equals("bold italic")) {
            weightInt = Typeface.BOLD_ITALIC;
        }
        else if (weight.equals("italic")) {
            weightInt = Typeface.ITALIC;
        }


        Typeface typeface = Typeface.create(family, weightInt);
        paint.setTypeface(typeface);

        int size = style.getInt("size");
        paint.setTextSize(size * 2);

        TextPaint textpaint = new TextPaint(paint);

        int paperWidth = width;

        Layout.Alignment align = Layout.Alignment.ALIGN_NORMAL;
        String alignString = style.getString("align");
        if (alignString.equals("center")){
            align = Layout.Alignment.ALIGN_CENTER;
        }
        else if (alignString.equals("opposite")){
            align = Layout.Alignment.ALIGN_OPPOSITE;
        }

        android.text.StaticLayout staticLayout = new StaticLayout(textToPrint, textpaint, paperWidth, align, 1, 0, false);
        int height = staticLayout.getHeight();


        Bitmap bitmap = Bitmap.createBitmap(staticLayout.getWidth(), height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bitmap);
        c.drawColor(Color.WHITE);
        c.translate(0, 0);
        staticLayout.draw(c);

        builder.appendBitmap(bitmap, true);
    }

    private static void cutPaper(ICommandBuilder builder){
        builder.appendCutPaper(CutPaperAction.PartialCutWithFeed);
    }

    private static byte [] createCommands(JSONObject params) throws JSONException {

        ICommandBuilder builder = StarIoExt.createCommandBuilder(Emulation.StarGraphic);
        builder.beginDocument();


        JSONArray commands = params.getJSONArray("commands");
        int widthPaper = params.getInt("paperWidth");

        for (int i = 0; i < commands.length(); i++) {
            JSONObject command = commands.getJSONObject(i);
            String type = command.getString("type");

            if (type.equals("text")) {
                createText(builder, command, widthPaper);
            }
             else if (type.equals("cutpaper")) {
                cutPaper(builder);
            }
            else if (type.equals("image")) {
                createImage(builder, command);
            }
            else if(type.equals("opencash")) {
                openCashDrawer(builder);
            }

        }


        builder.endDocument();
        return builder.getCommands();
    }

    private boolean sendCommand(Context context, String portName, String portSettings, JSONObject params, CallbackContext callbackContext) {
        StarIOPort port = null;

        try {
            port = StarIOPort.getPort(portName, portSettings, 10000, context);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }

            StarPrinterStatus status = port.beginCheckedBlock();

            if (true == status.offline) {
                sendEvent("printerOffline", "Printer is offline before start the process.");
                return false;
            }


            byte[] commandToSendToPrinter = createCommands(params);

            port.writePort(commandToSendToPrinter, 0, commandToSendToPrinter.length);

            port.setEndCheckedBlockTimeoutMillis(30000);// Change the timeout time of endCheckedBlock method.
            status = port.endCheckedBlock();

            if (status.coverOpen == true) {
                callbackContext.error("Cover open");
                sendEvent("printerCoverOpen", null);
                return false;
            } else if (status.receiptPaperEmpty == true) {
                callbackContext.error("Empty paper");
                sendEvent("printerPaperEmpty", null);
                return false;
            } else if (status.offline == true) {
                callbackContext.error("Printer offline");
                sendEvent("printerOffline", "Printer is offline after try print comands.");
                return false;
            }
            callbackContext.success("Printed");

        } catch (StarIOPortException e) {
            sendEvent("printerImpossible", e.getMessage());
            callbackContext.error(e.getMessage());

        } catch (JSONException e) {
            sendEvent("printerImpossible", e.getMessage());
            callbackContext.error(e.getMessage());
        }

        finally {
            if (port != null) {
                try {
                    StarIOPort.releasePort(port);
                } catch (StarIOPortException e) {
                    sendEvent("ImpossibleReleasePort", e.getMessage());
                    callbackContext.error(e.getMessage());
                }
            }
            return true;
        }
    }


    private static byte[] createCpUTF8(String inputText) {
        byte[] byteBuffer = null;

        try {
            byteBuffer = inputText.getBytes("CP437");
        } catch (UnsupportedEncodingException e) {
            byteBuffer = inputText.getBytes();
        }

        return byteBuffer;
    }


    private byte[] convertFromListByteArrayTobyteArray(List<byte[]> ByteArray) {
        int dataLength = 0;
        for (int i = 0; i < ByteArray.size(); i++) {
            dataLength += ByteArray.get(i).length;
        }

        int distPosition = 0;
        byte[] byteArray = new byte[dataLength];
        for (int i = 0; i < ByteArray.size(); i++) {
            System.arraycopy(ByteArray.get(i), 0, byteArray, distPosition, ByteArray.get(i).length);
            distPosition += ByteArray.get(i).length;
        }

        return byteArray;
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param dataType event type
     */
    private void sendEvent(String dataType, String info) {
        if (this._callbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(true);
            this._callbackContext.sendPluginResult(result);
        }
    }


}