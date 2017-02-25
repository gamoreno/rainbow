package org.sa.rainbow.brass.adaptation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.sa.rainbow.brass.model.map.EnvMap;
import org.sa.rainbow.brass.model.map.EnvMapNode;

/**
 * @author ashutosh
 *
 */
public class PolicyToIG {
    public PrismPolicy m_prismPolicy;
    public EnvMap m_map;

    public PolicyToIG(PrismPolicy policy, EnvMap map) {
        // TODO Auto-generated constructor stub
        m_prismPolicy = policy;
        m_map = map;
    }

    private String build_cmd(int cmd_id, double dest_x, double dest_y, double speed) {
        //V(1, do MoveAbs(23.5, 0, 1) then 2)
        NumberFormat f = new DecimalFormat("#0.0");
        String cmd = "V(" + cmd_id + ", do MoveAbs(" + 
                f.format(dest_x) + ", " + f.format(dest_y) + 
                ", " + speed + ") then " + ++cmd_id + ")";
        return cmd;
    }

    private String build_ig(ArrayList<String> cmds) {
        //P(V(1, do MoveAbs(23.5, 0, 1) then 2),
        //		  V(2, do MoveAbs(23.5, -10.5, 1) then 3)::
        //		  V(3, end)::
        //		  nil)
        String ins_graph = "P(";
        int i = 0;
        for (i = 0; i < cmds.size (); i++) {
            if (i == 0) {
                ins_graph += cmds.get (i) + ",\n";
            }
            else {
                ins_graph += cmds.get (i) + "::\n";
            }
        }
        // add the end
        ins_graph += "V(" + (i+1) + ", end)::\nnil)";

//		
//		for (i = 0; i < cmds.size(); i++) {
//			if (i == cmds.size() - 1) {
//				ins_graph += cmds.get(i) + "::\nV(" + (i + 2) 
//						+ ", end)::\nnil)";
//			} else if (i == 0) {
//				ins_graph += cmds.get(i) + ",\n";
//			} else {
//				ins_graph += cmds.get(i) + "::\n";
//			}
//		}

        return ins_graph;
    }

    public String translate() {
        ArrayList<String> plan = m_prismPolicy.getPlan();
        ArrayList<String> cmds = new ArrayList<String>();

        double speed = 1.0;
        int cmd_id = 1;

        for (int i = 0; i < plan.size(); i++) {
            String action = plan.get(i);

            // Heuristic for now.
            String[] elements = action.split("_");

            String destination = elements[2];
            String cmd = build_cmd(cmd_id, m_map.getNodeX(destination), m_map.getNodeY(destination), speed);
            cmds.add(cmd);
            ++cmd_id;			
        }

        String ins_graph = build_ig(cmds);
        System.out.println(ins_graph);

        return ins_graph;
    }


    public static String generateJSONWayPointList(PrismPolicy p, String r) {
        String out="{\"path\": [";
        for (int i=0; i<p.getPlan().size(); i++){
            String[] e = p.getPlan().get(i).split("_");
            if (i==0){
                out = out + "\""+e[0]+"\"";
            }
            out = out + ",\""+e[2]+"\"";
        }
        out = out +"], \"time\": "+r;
        out = out +"}";		
        return out;
    }

    public static void exportIGTranslation(String f, String s){
        try {
            BufferedWriter out = new BufferedWriter (new FileWriter(f));
            out.write(s);
            out.close();
        }
        catch (IOException e){
            System.out.println("Error exporting Instruction Graph translation");
        }
    }

    public static void main (String[] args) throws Exception { // Class test
        EnvMap map = new EnvMap (null, null);
        PrismConnector conn = new PrismConnector (null);
        String out_dir_ig="/Users/jcamara/Dropbox/Documents/Work/Projects/BRASS/rainbow-prototype/trunk/rainbow-brass/prismtmp/igs/";
        String out_dir_wp="/Users/jcamara/Dropbox/Documents/Work/Projects/BRASS/rainbow-prototype/trunk/rainbow-brass/prismtmp/paths/";
        for (EnvMapNode node_src : map.getNodes().values()) {
            for (EnvMapNode node_tgt : map.getNodes().values()) {
                if (node_src.getId()!=node_tgt.getId()){
                    System.out.println("Src:"+String.valueOf(node_src.getId())+" Tgt:"+String.valueOf(node_tgt.getId()));
                    conn.invoke (node_src.getId(),node_tgt.getId()); 
                    String prismResult = conn.getResult();
                    PrismPolicy prismPolicy = new PrismPolicy("/Users/jcamara/Dropbox/Documents/Work/Projects/BRASS/rainbow-prototype/trunk/rainbow-brass/prismtmp/botpolicy.adv");
                    prismPolicy.readPolicy();
                    //System.out.println(prismPolicy.getPlan().toString());
                    PolicyToIG translator = new PolicyToIG(prismPolicy, map);
                    // System.out.println(translator.translate());
                    exportIGTranslation(out_dir_ig+node_src.getLabel()+"_to_"+node_tgt.getLabel()+".ig",translator.translate());
                    String w =generateJSONWayPointList(prismPolicy, prismResult);
                    System.out.println(w);
                    exportIGTranslation(out_dir_wp+node_src.getLabel()+"_to_"+node_tgt.getLabel()+".json",w);  

                }
            }
        }
    }  
}
