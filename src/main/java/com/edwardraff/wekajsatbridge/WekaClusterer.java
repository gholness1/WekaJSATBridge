package com.edwardraff.wekajsatbridge;

/*
 * Copyright (C) 2014 Edward Raff
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import jsat.DataSet;
import jsat.clustering.ClustererBase;
import jsat.exceptions.FailedToFitException;
import weka.clusterers.Clusterer;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 *
 * @author Edward Raff
 */
public class WekaClusterer extends ClustererBase
{
    private Clusterer wekaClusterer;

    public WekaClusterer(Clusterer wekaClusterer)
    {
        this.wekaClusterer = wekaClusterer;
    }

    public WekaClusterer(WekaClusterer toCopy)
    {
        this.wekaClusterer = OtherUtils.serializationCopy(toCopy.wekaClusterer);
    }
    
    @Override
    public int[] cluster(DataSet arg0, int[] assignment)
    {
        Instances instances = InstanceHandler.dataSetToInstances(arg0);
        //cleanup might be needed first
        if(instances.classIndex() >= 0)
        {
            //Weka clusters don't like to cluster data if it has a class attribute. So we must remove it. 
            Remove removeFilter = new Remove();
            removeFilter.setInvertSelection(false);//only remove what I list
            removeFilter.setAttributeIndicesArray(new int[]{instances.classIndex()});
            
            try
            {
                removeFilter.setInputFormat(instances);
                instances = Filter.useFilter(instances, removeFilter);
            }
            catch (Exception ex)
            {
                Logger.getLogger(WekaClusterer.class.getName()).log(Level.SEVERE, null, ex);
                throw new FailedToFitException(ex);
            }
        }
        
        //ok, we are good now. 
        try
        {
            wekaClusterer.buildClusterer(instances);
            if(assignment == null || assignment.length < arg0.getSampleSize())
                assignment = new int[arg0.getSampleSize()];
            /*
             * weka demands this must be implemented, and since it dosn't return
             * the clustering of the input data - its the only way we can get 
             * the designations 
             */
            for(int i = 0; i < instances.numInstances(); i++)
                assignment[i] = wekaClusterer.clusterInstance(instances.instance(i));
            return assignment;
        }
        catch (Exception ex)
        {
            Logger.getLogger(WekaClusterer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FailedToFitException(ex);
        }
    }

    @Override
    public int[] cluster(DataSet arg0, ExecutorService arg1, int[] arg2)
    {
        return cluster(arg0, arg2);
    }

    @Override
    protected WekaClusterer clone() 
    {
        return new WekaClusterer(this);
    }
}
