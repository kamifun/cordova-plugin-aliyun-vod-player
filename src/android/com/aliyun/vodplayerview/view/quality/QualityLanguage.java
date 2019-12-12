package com.aliyun.vodplayerview.view.quality;

import android.content.Context;
import android.text.TextUtils;

public class QualityLanguage {

    public QualityLanguage() {

    }

    public static String getSaasLanguage(Context context, String quality) {
        if ("FD".equals(quality)) {
            return context.getString(context.getResources().getIdentifier("alivc_fd_definition", "string", context.getPackageName()));
        } else if ("LD".equals(quality)) {
            return context.getString(context.getResources().getIdentifier("alivc_ld_definition", "string", context.getPackageName()));
        } else if ("SD".equals(quality)) {
            return context.getString(context.getResources().getIdentifier("alivc_sd_definition", "string", context.getPackageName()));
        } else if ("HD".equals(quality)) {
            return context.getString(context.getResources().getIdentifier("alivc_hd_definition", "string", context.getPackageName()));
        } else if ("2K".equals(quality)) {
            return context.getString(context.getResources().getIdentifier("alivc_k2_definition", "string", context.getPackageName()));
        } else if ("4K".equals(quality)) {
            return context.getString(context.getResources().getIdentifier("alivc_k4_definition", "string", context.getPackageName()));
        } else if("SQ".equals(quality)){
            return context.getString(context.getResources().getIdentifier("alivc_sq_definition", "string", context.getPackageName()));
        } else if("HQ".equals(quality)){
            return context.getString(context.getResources().getIdentifier("alivc_hq_definition", "string", context.getPackageName()));
        } else {
            return context.getString(context.getResources().getIdentifier("alivc_od_definition", "string", context.getPackageName()));
        }
    }

    public static String getMtsLanguage(Context context, String quality) {
        if (TextUtils.isEmpty(quality)) {
            return null;
        } else {
            String xldStr;
            String item;
            if (quality.toUpperCase().contains("XLD")) {
                xldStr = context.getString(context.getResources().getIdentifier("alivc_mts_xld_definition", "string", context.getPackageName()));
                if (quality.contains("_")) {
                    item = quality.split("_")[1];
                    return xldStr + "_" + item;
                } else {
                    return xldStr;
                }
            }else if(quality.toUpperCase().contains("LD")){
                xldStr = context.getString(context.getResources().getIdentifier("alivc_mts_ld_definition", "string", context.getPackageName()));
                if(quality.contains("_")){
                    item = quality.split("_")[1];
                    return xldStr + "_" + item;
                }else{
                    return xldStr;
                }
            }else if(quality.toUpperCase().contains("SD")){
                xldStr = context.getString(context.getResources().getIdentifier("alivc_mts_sd_definition", "string", context.getPackageName()));
                if(quality.contains("_")){
                    item = quality.split("_")[1];
                    return xldStr + "_" + item;
                }else{
                    return xldStr;
                }
            }else if(quality.toUpperCase().contains("FHD")){
                xldStr = context.getString(context.getResources().getIdentifier("alivc_mts_fhd_definition", "string", context.getPackageName()));
                if(quality.contains("_")){
                    item = quality.split("_")[1];
                    return xldStr + "_" + item;
                }else{
                    return xldStr;
                }
            }else if(quality.toUpperCase().contains("HD")){
                xldStr = context.getString(context.getResources().getIdentifier("alivc_mts_hd_definition", "string", context.getPackageName()));
                if(quality.contains("_")){
                    item = quality.split("_")[1];
                    return xldStr + "_" + item;
                }else{
                    return xldStr;
                }
            }else{
                return null;
            }
        }
    }
}
