import net.percederberg.mibble.*;
import net.percederberg.mibble.value.ObjectIdentifierValue;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;


public class Test {
    public static String READ_COMMUNITY = "public";
    MibLoader loader = new MibLoader();

    public static void main(String[] args) {
        try {
            String strIPAddress = "192.168.178.38";
            Test objSNMP = new Test();
            Scanner eingabe=new Scanner(System.in);
            System.out.println("Welchen Community String moechtest du verwenden? public oder private");
            READ_COMMUNITY=eingabe.nextLine();

            objSNMP.snmpGet(strIPAddress, READ_COMMUNITY, "1.3.6.1.2.1.1.1.0");
            objSNMP.snmpGet(strIPAddress, READ_COMMUNITY, "1.3.6.1.2.1.1.2.0");
            objSNMP.snmpGet(strIPAddress, READ_COMMUNITY, "1.3.6.1.2.1.1.3.0");
            objSNMP.snmpGet(strIPAddress, READ_COMMUNITY, "1.3.6.1.2.1.1.4.0");
            objSNMP.snmpGet(strIPAddress, READ_COMMUNITY, "1.3.6.1.2.1.1.5.0");
            objSNMP.snmpGet(strIPAddress, READ_COMMUNITY, "1.3.6.1.2.1.1.6.0");



            while (true){
                System.out.println("Was moechtest du machen? get: Information bekommen; scan: ganzes Netzwerk scannen; loadMib: MibFile ausgeben;");
                String benutzerEingabe=eingabe.nextLine();
                if (benutzerEingabe.equals("get")){
                    System.out.println("Gib eine OID oder einen Namen einer OID ein.");
                    String OID=eingabe.nextLine();
                    OID=objSNMP.readMib(OID);
                    objSNMP.snmpGet(strIPAddress,READ_COMMUNITY,OID);
                } else if(benutzerEingabe.equals("scan")){
                    System.out.println("Gib eine OID oder einen Namen einer OID ein.");
                    String OID=eingabe.nextLine();
                    OID=objSNMP.readMib(OID);
                    objSNMP.getNetwork(OID);
                } else if(benutzerEingabe.equals("loadMib")){
                    System.out.println("Gib den Namen der Mibfile ein.");
                    String name=eingabe.nextLine();
                    objSNMP.loadMib(name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public String readMib(String eingabe) throws IOException, MibLoaderException {

        Mib mib1=loader.load("RFC1213-MIB");
        Mib mib2=loader.load("HOST-RESOURCES-MIB");
        HashMap<String, ObjectIdentifierValue> map1 = extractOids(mib1);
        HashMap<String, ObjectIdentifierValue> map2 = extractOids(mib2);

        if (map1.containsKey(eingabe)){
            return map1.get(eingabe).toString()+".0";
        } else if(map2.containsKey(eingabe)){
            return map2.get(eingabe).toString()+".0";
        } else {
            return eingabe;
        }
    }

    public void loadMib(String name) throws IOException, MibLoaderException {
        Mib mib=loader.load(name);
        HashMap<String, ObjectIdentifierValue> map3 = extractOids(mib);
        System.out.println(map3);
    }



    public static HashMap<String, ObjectIdentifierValue> extractOids(Mib mib) {
        HashMap<String,ObjectIdentifierValue> map = new HashMap<>();
        for (Object symbol : mib.getAllSymbols()) {
            ObjectIdentifierValue oid = extractOid((MibSymbol) symbol);
            if (oid != null) {
                map.put(((MibSymbol) symbol).getName(), oid);
            }
        }
        return map;
    }

    public static ObjectIdentifierValue extractOid(MibSymbol symbol) {
        if (symbol instanceof MibValueSymbol) {
            MibValue value = ((MibValueSymbol) symbol).getValue();
            if (value instanceof ObjectIdentifierValue) {
                return (ObjectIdentifierValue) value;
            }
        }
        return null;
    }



    public void getNetwork(String OID){
        String network="192.168.178.0";
        String[] parts = network.split(Pattern.quote("."));
        String test;

        for (Integer i=0; i<256; ++i){
            parts[3]= i.toString();
            test="192.168.178.";
            test=test.concat(parts[3]);
            try {
                InetAddress address = InetAddress.getByName(test);
                if (address.isReachable(1000)) {
                    String output = address.toString().substring(1);
                    System.out.print("Die Adresse: "+output + " ist im Netzwerk!");
                    snmpGet(output,READ_COMMUNITY,OID);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }




    public String snmpGet(String strAddress, String community, String strOID) {
        String antwort="";
        try {
            OctetString community1 = new OctetString(community);

            strAddress= strAddress+"/" + 161;

            Address targetaddress = new UdpAddress(strAddress);
            TransportMapping transport = new DefaultUdpTransportMapping();
            transport.listen();

            CommunityTarget comtarget = new CommunityTarget();
            comtarget.setCommunity(community1);
            comtarget.setVersion(SnmpConstants.version1);
            comtarget.setAddress(targetaddress);
            comtarget.setRetries(2);
            comtarget.setTimeout(5000);

            PDU pdu = new PDU();
            ResponseEvent response;
            Snmp snmp;

            pdu.add(new VariableBinding(new OID(strOID)));
            pdu.setType(PDU.GET);
            snmp = new Snmp(transport);
            response = snmp.get(pdu,comtarget);

            if(response != null) {

                try {
                    if(response.getResponse().getErrorStatusText().equalsIgnoreCase("Success")) {
                        PDU pduresponse=response.getResponse();
                        antwort=pduresponse.getVariableBindings().firstElement().toString();
                        if(antwort.contains("=")) {
                            int len = antwort.indexOf("=");
                            antwort=antwort.substring(len+1, antwort.length());
                        }
                    }
                    System.out.println(" Information dieses Gerätes= "+antwort);
                }catch (Exception e){
                    System.err.println(" Diese IP unterstüzt SNMP nicht!");
                }

            } else {
                System.out.println("Feeling like a TimeOut occured ");
            }
            snmp.close();
        } catch(Exception e) { e.printStackTrace(); }
        return antwort;
    }
}