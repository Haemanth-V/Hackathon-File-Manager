package com.example.hackathonfilemanager;

import android.content.Context;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends BaseAdapter implements Filterable {

    private static final String TAG = "FileAdapter";
    private List<String> fileNames = new ArrayList<>();
    private List<String> fileNamesAll;
    private Context context;
    private boolean[] selection;

    public FileAdapter(List<String> fileNames, boolean[] selection, Context context) {
        this.fileNames = fileNames;
        this.selection = selection;
        this.context = context;
        fileNamesAll = new ArrayList<>(fileNames);
    }

    @Override
    public int getCount() {
        return fileNames.size();
    }

    @Override
    public String getItem(int position) {
        return fileNames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
        fileNamesAll = new ArrayList<>(fileNames);
        notifyDataSetChanged();
    }

    public void setSelection(boolean[] selection) {
        this.selection = selection;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView  == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.file_item, parent,false);
            convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.textViewFileItem),
                    (ImageView) convertView.findViewById(R.id.imageIcon),
                    (LinearLayout) convertView.findViewById(R.id.fileItem)));
        }
        ViewHolder holder = (ViewHolder) convertView.getTag();
        final String item = getItem(position);
        holder.name.setText(item.substring(item.lastIndexOf('/')+1));
        File file = new File(item);
        if(file.isDirectory()){
            holder.image.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_folder));
        }
        else{
            holder.image.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file));
        }
        if(selection != null) {
            if(selection[position]) {
                holder.layout.setBackgroundColor(Color.argb(50,9,9,9));
                Log.d(TAG, "getView: GRAY SET");
            }else{
                holder.layout.setBackgroundColor(Color.WHITE);
                Log.d(TAG, "getView: WHITE SET");
            }
        }
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    private Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<String> filteredList = new ArrayList<>();
            if(constraint == null || constraint.length()==0){
                filteredList.addAll(fileNamesAll);
            }else{
                String filterPattern = constraint.toString().toLowerCase().trim();
                for(String s : fileNamesAll){
                    if(s.toLowerCase().contains(filterPattern)){
                        filteredList.add(s);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }


        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            fileNames.clear();
            fileNames.addAll((List)results.values);
            notifyDataSetChanged();
        }
    };

    class ViewHolder{
        TextView name;
        ImageView image;
        LinearLayout layout;
        ViewHolder(TextView name, ImageView image, LinearLayout layout){
            this.name = name;
            this.image = image;
            this.layout = layout;
        }
    }
}
