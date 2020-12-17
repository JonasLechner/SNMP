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
import java.util.Scanner;
import java.util.regex.Pattern;

public class Test {
    public static final String READ_COMMUNITY = "public";
    public static final String OID_SYS_DESCR="1.3.6.1.2.1.1.5.0";
    public static void main(String[] args) {
        try {


            String strIPAddress = "192.168.178.38";
            Test objSNMP = new Test();

            while (true){
                System.out.println("Was moechtest du machen? get: sysname eines Gerätes bekommen! scan: ganzes Netzwerk scannen!");
                Scanner eingabe=new Scanner(System.in);
                String benutzerEingabe=eingabe.nextLine();
                if (benutzerEingabe.equals("get")){
                    String antwort =objSNMP.snmpGet(strIPAddress,READ_COMMUNITY,OID_SYS_DESCR);
                } else if(benutzerEingabe.equals("scan")){
                    objSNMP.getNetwork();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void getNetwork(){
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
                    snmpGet(output,READ_COMMUNITY,OID_SYS_DESCR);
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
                    System.out.println(" Der Name dieses Gerätes ist="+antwort);
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