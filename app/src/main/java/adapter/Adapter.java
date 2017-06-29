package adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yorhp.refreshview.R;

import java.util.ArrayList;

/**
 * Created by Tyhj on 2017/6/29.
 */

public class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

    ArrayList<String> arrayList=new ArrayList<>();
    Context context;
    LayoutInflater inflater;

    public Adapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        arrayList.add("a");
        arrayList.add("b");
        arrayList.add("c");
        arrayList.add("d");
        arrayList.add("e");
        arrayList.add("f");
        arrayList.add("g");
        arrayList.add("h");
        arrayList.add("i");
        arrayList.add("j");
        arrayList.add("k");
        arrayList.add("l");
        arrayList.add("m");
        arrayList.add("n");
        arrayList.add("o");
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view=inflater.inflate(R.layout.item_list,parent,false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        holder.tv_name.setText(arrayList.get(position));
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    class Holder extends RecyclerView.ViewHolder{
        TextView tv_name;
        public Holder(View itemView) {
            super(itemView);
            tv_name= (TextView) itemView.findViewById(R.id.tv_name);
        }
    }
}
