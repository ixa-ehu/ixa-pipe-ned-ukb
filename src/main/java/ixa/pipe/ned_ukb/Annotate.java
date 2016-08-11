/*
 * Copyright (C) 2016 IXA Taldea, University of the Basque Country UPV/EHU

   This file is part of ixa-pipe-ned-ukb.
                                                                    
   ixa-pipe-ned-ukb is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   ixa-pipe-ned-ukb is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License 
   along with ixa-pipe-ned-ukb.  If not, see <http://www.gnu.org/licenses/>.
*/


package ixa.pipe.ned_ukb;

import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;
import ixa.kaflib.Term;
import ixa.kaflib.ExternalRef;
import ixa.kaflib.Entity;
import org.apache.commons.lang.StringEscapeUtils;


public class Annotate {

    boolean cross = false;
    DictManager crosslinkMappingIndex;
    String crosslinkMappingHashName;
    String language;
    String resourceMapping;


    public Annotate(String crosslinkMappingIndexFile, String language) throws Exception{
	this.language = language;
	this.crosslinkMappingHashName = language + "En";
	if((!language.equals("en")) && (crosslinkMappingIndexFile != null) && (!crosslinkMappingIndexFile.equals("none"))){
	    if(! new File(crosslinkMappingIndexFile).exists()) {
		throw new Exception("The following database specified by \"CrossWikipediaIndex\" not found: " + crosslinkMappingIndexFile);
	    }
	    crosslinkMappingIndex = new DictManager(crosslinkMappingIndexFile, this.crosslinkMappingHashName);
	    this.cross = true;
	    this.resourceMapping = crosslinkMappingIndexFile.substring(crosslinkMappingIndexFile.lastIndexOf("/") + 1);
	}
    }
    

    public void disambiguateNEsToKAF(KAFDocument kaf, String scripts, String ukbExec, String ukbKb, String ukbDict, String wikiDb) throws Exception {

	String resourceExternalRef = ukbKb.substring(ukbKb.lastIndexOf("/") + 1);

	List<String> neIds = new ArrayList<String>();
	String ukbContext = "naf\n";
	
	List<Entity> entities = kaf.getEntities();
	for (Entity entity : entities){
	    String entityId = entity.getId();
	    String entityLemma = "";
	    List<Term> entityTerms = entity.getTerms();
	    for (Term term : entityTerms){
		String tId = term.getId();
		neIds.add(tId);
		if(!entityLemma.equals("")){
		    entityLemma += "_";
		}
		entityLemma += term.getLemma().toLowerCase();
	    }
	    ukbContext += entityLemma + "##" + entityId + "#1 ";    
	}

	String formsContext2Match = "";
	String lemmasContext2Match = "";

	List<Term> terms = kaf.getTerms();
	for (Term term : terms){
	    if(!neIds.contains(term.getId())){
		if(!(term.getForm().contains("@@")) && !(term.getForm().contains(" "))){
		    formsContext2Match += term.getForm().toLowerCase() + "@@" + term.getWFs().get(0).getOffset() + " ";
		    lemmasContext2Match += term.getLemma().toLowerCase() + "@@" + term.getWFs().get(0).getOffset() + " ";
		}
	    }
	}
	
	// create UKB context
	String[] cmdMatch = {
	    "perl",
	    scripts + "/merge_match.pl",
	    "-d",
	    wikiDb,
	    "--t1",
	    formsContext2Match,
	    "--t2",
	    lemmasContext2Match
	};

	Process pMatch = Runtime.getRuntime().exec(cmdMatch);

	String matchedContext = "";
	String outputLineContext = "";
	BufferedReader outputContextStream = new BufferedReader(new InputStreamReader(pMatch.getInputStream(), "UTF-8"));
	while((outputLineContext = outputContextStream.readLine()) != null){
	    matchedContext += outputLineContext + "\n";
	}
	outputContextStream.close();

	String errorContext = "";
	BufferedReader errorContextStream = new BufferedReader(new InputStreamReader(pMatch.getErrorStream()));
	while(( errorContext = errorContextStream.readLine()) != null){
	    System.err.println("MERGE_MATCH ERROR: " + errorContext);
	}
	errorContextStream.close();

	pMatch.waitFor();

	String[] contextStrings = matchedContext.split(" ");
	for(String contextString : contextStrings){
	    contextString = contextString.trim();

	    //ContextString = spot_string@@spot_offset
	    String[] contextWordOffset = contextString.split("@@");
	    ukbContext += contextWordOffset[0] + "##" + contextWordOffset[1] + "#1 ";
	}

	File contextTmpFile = File.createTempFile("context", ".tmp");
	contextTmpFile.deleteOnExit();
	String contextTmpFileName = contextTmpFile.getAbsolutePath();

	Writer contextFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(contextTmpFile), "UTF-8"));
	try {
	    contextFile.write(ukbContext);
	} finally {
	    contextFile.close();
	}


	// run UKB
	String cmdUkb = ukbExec + " --prank_damping 0.90 --prank_iter 15 --allranks --minput --nopos --ppr_w2w --dict_weight -K " + ukbKb + " -D " + ukbDict + " " + contextTmpFileName;
	
	Process pUkb = Runtime.getRuntime().exec(cmdUkb);

	String outputUkb = "";
	String outputLineUkb = "";
	BufferedReader outputUkbStream = new BufferedReader(new InputStreamReader(pUkb.getInputStream(), "UTF-8"));
	while((outputLineUkb = outputUkbStream.readLine()) != null){
	    outputUkb += outputLineUkb + "\n";
	}
	outputUkbStream.close();

	String errorUkb = "";
	BufferedReader errorUkbStream = new BufferedReader(new InputStreamReader(pUkb.getErrorStream()));
	while((errorUkb = errorUkbStream.readLine()) != null){
	    System.err.println("UKB ERROR: " + errorUkb);
	}
	errorUkbStream.close();

	pUkb.waitFor();
	
	// UKB output (one line): context_id word_id (concept_id(/weight)?)+ !! lemma   (there are 2 spaces after word_id)
	// UKB output example:    naf e12  Norvegia/0.999998 Norvegiako_bandera/2.25207e-06 !! norvegia
	Map<String, String> entityLinks = new HashMap<String, String>(); // e12 --> Norvegia/0.999998
	String ukbDisambiguations[] = outputUkb.split("\n");
	for( String ukbDisambiguation : ukbDisambiguations){
	    if(ukbDisambiguation.startsWith("!! -v")) continue;
	    String ukbLine[] = ukbDisambiguation.split(" ");
	    entityLinks.put(ukbLine[1], ukbLine[3]);
	}

	// UKB links to KAF
	for (Entity entity : entities){
	   String entityId = entity.getId();
	   if(entityLinks.containsKey(entityId)){
	       String reference = entityLinks.get(entityId).split("/")[0];
	       String confidence = entityLinks.get(entityId).split("/")[1];
	       String ref2 = reference;
	       reference = "http://" + language + ".wikipedia.org/wiki/" + reference;
	       ExternalRef externalRef = kaf.newExternalRef(resourceExternalRef,reference);
	       externalRef.setConfidence(Float.parseFloat(confidence));
	       externalRef.setSource(language);
	       externalRef.setReftype(language);
	       entity.addExternalRef(externalRef);
	       if(cross){
		   String mappingRef = getMappingRef(reference);
		   if(mappingRef != null){
		       ExternalRef enRef = kaf.newExternalRef(this.resourceMapping, mappingRef);
		       enRef.setConfidence(Float.parseFloat(confidence));
		       enRef.setSource(language);
		       enRef.setReftype("en");
		       entity.addExternalRef(enRef);
		   }
	       }		
	   }
	   else{ // UKB didn't assign any link to this entity. Try with MFS
	       	String cmdMfs = "perl " + scripts + "/mfs.pl -d " + wikiDb;
		Process pMfs = Runtime.getRuntime().exec(cmdMfs);

		String entityLemma = "";
		List<Term> entityTerms = entity.getTerms();
		for (Term term : entityTerms){
		    if(!entityLemma.equals("")){
			entityLemma += "_";
		    }
		    entityLemma += term.getLemma().toLowerCase();
		}

		OutputStream stdinMfs = pMfs.getOutputStream();
		stdinMfs.write(entityLemma.getBytes());
		stdinMfs.flush();
		stdinMfs.close();

		String outputMfs = "";
		BufferedReader outputMfsStream = new BufferedReader(new InputStreamReader(pMfs.getInputStream(), "UTF-8"));
		outputMfs = outputMfsStream.readLine();
		outputMfsStream.close();

		String errorMfs = "";
		BufferedReader errorMfsStream = new BufferedReader(new InputStreamReader(pMfs.getErrorStream()));
		while((errorMfs = errorMfsStream.readLine()) != null){
		    System.err.println("MFS ERROR: " + errorMfs);
		}
		errorMfsStream.close();

		pMfs.waitFor();
		if(!outputMfs.equals("NILL")){
		    String reference = outputMfs;
		    String confidence = "1";
		    reference = "http://" + language + ".wikipedia.org/wiki/" + reference;
		    ExternalRef externalRef = kaf.newExternalRef("MFS_" + resourceExternalRef,reference);
		    externalRef.setConfidence(Float.parseFloat(confidence));
		    externalRef.setSource(language);
		    externalRef.setReftype(language);
		    entity.addExternalRef(externalRef);
		    if(cross){
			String mappingRef = getMappingRef(reference);
			if(mappingRef != null){
			    ExternalRef enRef = kaf.newExternalRef(this.resourceMapping, mappingRef);
			    enRef.setConfidence(Float.parseFloat(confidence));
			    enRef.setSource(language);
			    enRef.setReftype("en");
			    entity.addExternalRef(enRef);
			}
		    }		

		}
	   }
	}

    }


    private String getMappingRef(String ref){
	String[] info = ref.split("/");
	int pos = info.length - 1;
	String entry = info[pos];
	String url = "http://en.wikipedia.org/wiki/";
	String value = crosslinkMappingIndex.getValue(entry);
	if (value != null){
	    value = value.replace(" ","_");
	    return url + value;
	}
	return null;
    }

}
