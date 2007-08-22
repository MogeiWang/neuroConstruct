/**
 * neuroConstruct
 *
 * Software for developing large scale 3D networks of biologically realistic neurons
 * Copyright (c) 2007 Padraig Gleeson
 * UCL Department of Physiology
 *
 * Development of this software was made possible with funding from the
 * Medical Research Council
 *
 */

package ucl.physiol.neuroconstruct.mechanisms;

import java.io.*;
import java.util.*;

import ucl.physiol.neuroconstruct.project.*;
import ucl.physiol.neuroconstruct.utils.*;
import ucl.physiol.neuroconstruct.utils.units.*;
import ucl.physiol.neuroconstruct.simulation.SimEnvHelper;

/**
 * Class holding info on the file which will implement the cell mechanism, e.g. a mod file
 * for NEURON
 *
 * @author Padraig Gleeson
 *  
 */

public class MechanismImplementation
{
    ClassLogger logger = new ClassLogger("MechanismImplementation");

    String simulationEnvironment = null;

    String implementingFile = null;

    int unitSystem;


    public static final String startToken = "%";
    public static final String endToken = "%";

    private static final String nameToken = "Name";

    private Hashtable paramValueSubstitutes = new Hashtable();


    public MechanismImplementation()
    {

    }

    public MechanismImplementation(String simulationEnvironment,
                                 String implementingFile)
    {
        this.implementingFile = implementingFile;
        this.simulationEnvironment = simulationEnvironment;
    }



    public void createMechanismFile(AbstractedCellMechanism cellMechanism,
                                  int unitSystem,
                                  File fileToGenerate,
                                  Project project) throws FileNotFoundException, IOException
    {
        logger.logComment("Creating mech file: "+ fileToGenerate+" for cellMechanism: "+cellMechanism);

        Reader fileIn = new FileReader(getImplementingFileObject(project, cellMechanism.getInstanceName()));

        this.unitSystem = unitSystem;

        FileWriter fileOut = new FileWriter(fileToGenerate);

        String commentBlockStart = null;
        String commentBlockEnd = null;
        String commentLinePrefix = null;

        if (simulationEnvironment.equals(SimEnvHelper.NEURON))
        {
            commentBlockStart = "COMMENT";
            commentBlockEnd = "ENDCOMMENT";
            commentLinePrefix = "   ";
        }
        else if (simulationEnvironment.equals(SimEnvHelper.GENESIS))
        {
            commentBlockStart = "";
            commentBlockEnd = "";
            commentLinePrefix = "// ";
        }



        fileOut.write(commentBlockStart+"\n\n");
        fileOut.write(commentLinePrefix+"**************************************************\n");
        fileOut.write(commentLinePrefix+"File generated by: neuroConstruct v"+GeneralProperties.getVersionNumber()+" \n");
        fileOut.write(commentLinePrefix+"**************************************************\n\n");


        fileOut.write(commentLinePrefix+"This file holds the implementation in "+this.simulationEnvironment+" of the Cell Mechanism:\n");
        fileOut.write(commentLinePrefix+cellMechanism.getInstanceName()
            + " (Type: " + cellMechanism.getMechanismType()
            + ", Model: " + cellMechanism.getMechanismModel()+")\n\n");
        fileOut.write(commentLinePrefix+"with parameters: \n");

        InternalPhysicalParameter[] parameterList = cellMechanism.getPhysicalParameterList();

        for (int i = 0; i < parameterList.length; i++)
        {
            //float realValue = Units.;
            fileOut.write(commentLinePrefix+parameterList[i].getParameterName()
                          +" = "
                          +parameterList[i].getValue()
                          +" "
                          + parameterList[i].getUnits().getSafeSymbol()
                          + " \n");
        }

        fileOut.write("\n"+commentBlockEnd+"\n");
        fileOut.write("\n");

        LineNumberReader reader = new LineNumberReader(fileIn);
        String nextLine = null;

        while ( (nextLine = reader.readLine()) != null)
        {
            String newLine = replaceParameters(cellMechanism, nextLine)+"\n";
           // logger.logComment(">>>  "+ newLine);
            fileOut.write(newLine);

        }
        fileIn.close();
        fileOut.close();

    }

    /**
     * Do some tricks to ensure the file is located.
    */
    public File getImplementingFileObject(Project project, String cellMechanismName)
    {
        File proposedImplFile = new File(implementingFile);

        //System.out.println("...............          Getting the REAL impl file for "+cellMechanismName+" as in project "+ project.getProjectName()
        //                  + ", stored file string: "+ implementingFile);

       if (cellMechanismName==null)
       {
           logger.logError("Null name for cellMechanismName... ");
           return null;
       }

        String fileNameOnly = proposedImplFile.getName();

        File targetDir = ProjectStructure.getCellMechanismDir(project.getProjectMainDirectory());
        File idealFileLocation = null;

        if (targetDir.exists())
        {
            File idealDir = new File(targetDir, cellMechanismName);

            idealFileLocation = new File(idealDir,
                                              fileNameOnly);

            if (idealFileLocation.exists())
            {
                implementingFile = fileNameOnly;
                logger.logComment("File is where it should be: " + idealFileLocation.getAbsolutePath() +
                                  ". Setting stored file name to: " + implementingFile);
                return idealFileLocation;
            }
            else
            {
                System.out.println("Not found in: "+ idealFileLocation.getAbsolutePath());
            }
        }
        // try old way...

        targetDir = ProjectStructure.getFileBasedCellProcessesDir(project.getProjectMainDirectory(), false);


        idealFileLocation = new File(targetDir,
                                      fileNameOnly);

        if (idealFileLocation.exists())
        {
            implementingFile = fileNameOnly;
            logger.logComment("File is where it should be. Setting stored file name to: "+ implementingFile);
            return idealFileLocation;
        }

        if (!idealFileLocation.exists())
        {
            logger.logError("That impl file: "+idealFileLocation+" doesn't exist!!");
            // code to update from previous way to store files...
            File templateDir = null;
            if (simulationEnvironment.equals(SimEnvHelper.NEURON))
            {
                templateDir = ProjectStructure.getModTemplatesDir();
            }
            else if (simulationEnvironment.equals(SimEnvHelper.GENESIS))
            {
                templateDir = ProjectStructure.getGenesisTemplatesDir();
            }

            File supposedLoc = new File(templateDir, fileNameOnly);

            if (supposedLoc.exists())
            {
                logger.logComment("Think it's at: "+supposedLoc.getAbsolutePath());
                try
                {
                    File newLoc = GeneralUtils.copyFileIntoDir(supposedLoc, targetDir);
                    logger.logComment("New loc: "+ newLoc);
                    return newLoc;
                }
                catch (IOException ex)
                {
                    GuiUtils.showErrorMessage(logger, "Error updating location of file: "+ implementingFile
                                              + ". Tried to move it from "+supposedLoc +" to "+ idealFileLocation, ex, null);
                }
            }

            GuiUtils.showErrorMessage(logger, "Error getting location of file: "+ implementingFile, null, null);


/*
            logger.logComment("importedCellProcDir: "+ importedCellProcDir);

            if (implFile.getParentFile()!=null &&
                implFile.getParentFile().exists() &&
                implFile.getParentFile().getName().equals(importedCellProcDir.getName()))
            {
                implFile = new File(importedCellProcDir, implFile.getName());
                logger.logComment("Suggestion 1: "+ implFile);
            }
            else
            {
                implFile = new File(importedCellProcDir, implFile.getName());
                logger.logComment("Suggestion 2: "+ implFile);
            }
            if (implFile.exists())
            {
                this.implementingFile = implFile.getAbsolutePath();
                project.markProjectAsEdited();
            }
*/
        }
        return null;
    }

    public static String getNamePlaceholder()
    {
        return startToken + nameToken + endToken;
    }

    /**
     * Allows a string to replace the given values of a parameter
     */
    public void addParamValueSubstitute(String paramName, float value, String subsString)
    {
        Hashtable valueStrings = null;
        if (!paramValueSubstitutes.containsKey(paramName))
        {
            valueStrings = new Hashtable();
            paramValueSubstitutes.put(paramName, valueStrings);
        }
        valueStrings = (Hashtable)paramValueSubstitutes.get(paramName);

        valueStrings.put(new Float(value), subsString);
    }



    private String replaceParameters(AbstractedCellMechanism cellMechanism, String oldLine)
    {
        String alteredLine = new String(oldLine);


        //logger.logComment("Replacing stuff in line: "+ oldLine);


        alteredLine = GeneralUtils.replaceAllTokens(alteredLine,
                                                        getNamePlaceholder(),
                                                        cellMechanism.getInstanceName());


        InternalPhysicalParameter[] parameterList = cellMechanism.getPhysicalParameterList();
   /*
        int nextTokenStart;

        while ((nextTokenStart = oldLine.indexOf(startToken))>0)
        {
            logger.logComment("Looking at current line: "+ oldLine);
            int nextTokenFinish = oldLine.indexOf(endToken, nextTokenStart+1);
            if (nextTokenFinish<0)
            {
                logger.logError("Odd number of token delineators");
                oldLine = oldLine + "** Warning! Incomplete token. Should start with "
                    + startToken + " and end with " + endToken;
                return oldLine;
            }
           // String contentsToken = oldLine.substring()

        }
*/
        for (int i = 0; i < parameterList.length; i++)
        {
            String paramName = parameterList[i].getParameterName();

            String searchString = startToken + paramName + endToken;

            String replacementString = null;

            if (!paramValueSubstitutes.containsKey(paramName))
            {
                PhysicalQuantity realVal =
                    UnitConverter.convertFromNeuroConstruct(parameterList[i].getValue(),
                                                            parameterList[i].getUnits(),
                                                            unitSystem);

                replacementString = realVal.getMagnitude() +"";

            }
            else
            {
                Hashtable valueStrings = (Hashtable)paramValueSubstitutes.get(paramName);

                replacementString
                  = (String)valueStrings.get(new Float(parameterList[i].getValue()));

                if (replacementString==null)
                {
                    replacementString
                        = "** ERROR! Replacement String when parameter "
                        + paramName +" has value "+
                        parameterList[i].getValue()
                        +" not found! **";
                }
            }


            alteredLine = GeneralUtils.replaceAllTokens(alteredLine,
                                                        searchString,
                                                        replacementString);

        }

        return alteredLine;
    }


    public String getImplementingFile()
    {
        return implementingFile;
    }


    public String getSimulationEnvironment()
    {
        return simulationEnvironment;
    }

    public void setImplementingFile(String implementingFile)
    {
        this.implementingFile = implementingFile;
    }

    public void setSimulationEnvironment(String simulationEnvironment)
    {
        this.simulationEnvironment = simulationEnvironment;
    }

    public static void main(String[] args) throws CellMechanismException
    {
        PassiveMembraneMechanism mech = new PassiveMembraneMechanism();

        mech.setInstanceName("Passivo");

        mech.setParameter(PassiveMembraneMechanism.REV_POTENTIAL, 666);

        File newFile = new File("c:\\temp\\proc.p");



    }
    public Hashtable getParamValueSubstitutes()
    {
        return paramValueSubstitutes;
    }
    public void setParamValueSubstitutes(Hashtable paramValueSubstitutes)
    {
        this.paramValueSubstitutes = paramValueSubstitutes;
    }

}
