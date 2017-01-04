package com.celuk.database.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.celuk.database.model.CelukRequest;
import com.celuk.main.R;

/**
 * Created by adikwidiasmono on 11/20/16.
 */

public class CelukReceiverRequestViewHolder extends RecyclerView.ViewHolder {
    public ImageView ivReqAccept, ivReqReject;
    private View vIndicator;
    private TextView tvCallerEmail, tvReqDate, tvReqStatus;

    public CelukReceiverRequestViewHolder(View itemView) {
        super(itemView);

        vIndicator = itemView.findViewById(R.id.v_caller_indicator);
        tvCallerEmail = (TextView) itemView.findViewById(R.id.tv_caller_email);
        tvReqDate = (TextView) itemView.findViewById(R.id.tv_req_date);
        tvReqStatus = (TextView) itemView.findViewById(R.id.tv_req_receiver_status);
        ivReqAccept = (ImageView) itemView.findViewById(R.id.iv_req_accept);
        ivReqReject = (ImageView) itemView.findViewById(R.id.iv_req_reject);
    }

    public void bindToHolder(CelukRequest celukRequest) {
        if (!celukRequest.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_PENDING)) {
            ivReqAccept.setVisibility(View.GONE);
            ivReqReject.setVisibility(View.GONE);
            tvReqStatus.setVisibility(View.VISIBLE);
            tvReqStatus.setText(celukRequest.getStatus());
        } else {
            ivReqAccept.setVisibility(View.VISIBLE);
            ivReqReject.setVisibility(View.VISIBLE);
            tvReqStatus.setVisibility(View.GONE);
        }

        tvCallerEmail.setText(celukRequest.getReceiver());
        tvReqDate.setText(celukRequest.getRequestedDate());
    }

}
