package com.app.secondstory.adapter;

import android.app.Activity;
import android.content.Context;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.app.secondstory.R;
import com.app.secondstory.data.Constant;
import com.app.secondstory.model.ProductImage;
import com.app.secondstory.utils.Tools;
import com.balysv.materialripple.MaterialRippleLayout;

import java.util.List;

public class AdapterProductImage extends PagerAdapter {

    private Activity act;
    private List<ProductImage> items;

    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, ProductImage obj, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    // constructor
    public AdapterProductImage(Activity activity, List<ProductImage> items) {
        this.act = activity;
        this.items = items;
    }

    @Override
    public int getCount() {
        return this.items.size();
    }

    public ProductImage getItem(int pos) {
        return items.get(pos);
    }

    public void setItems(List<ProductImage> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        final ProductImage o = items.get(position);
        LayoutInflater inflater = (LayoutInflater) act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.item_product_image, container, false);
        ImageView image = (ImageView) v.findViewById(R.id.image);
        MaterialRippleLayout lyt_parent = (MaterialRippleLayout) v.findViewById(R.id.lyt_parent);
        Tools.displayImageOriginal(act, image, Constant.getURLimgProduct(o.name));
        lyt_parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(v, o, position);
                }
            }
        });

        ((ViewPager) container).addView(v);

        return v;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((RelativeLayout) object);

    }

}
