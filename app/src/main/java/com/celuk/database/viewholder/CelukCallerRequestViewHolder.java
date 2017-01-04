package com.celuk.database.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.celuk.database.model.CelukRequest;
import com.celuk.main.R;

/**
 * Created by adikwidiasmono on 11/20/16.
 */

public class CelukCallerRequestViewHolder extends RecyclerView.ViewHolder {
    private View vIndicator;
    private TextView tvReceiverEmail, tvReqDate, tvStatus;

    public CelukCallerRequestViewHolder(View itemView) {
        super(itemView);

        vIndicator = itemView.findViewById(R.id.v_receiver_indicator);
        tvReceiverEmail = (TextView) itemView.findViewById(R.id.tv_receiver_email);
        tvReqDate = (TextView) itemView.findViewById(R.id.tv_req_date);
        tvStatus = (TextView) itemView.findViewById(R.id.tv_req_receiver_status);
    }

    public void bindToHolder(CelukRequest celukRequest) {
        tvReceiverEmail.setText(celukRequest.getReceiver());
        tvReqDate.setText(celukRequest.getRequestedDate());
        tvStatus.setText(celukRequest.getStatus());
        switch (celukRequest.getStatus()) {
            case CelukRequest.REQUEST_STATUS_PENDING:
                vIndicator.setBackgroundResource(R.color.c_yellow);
                break;
            case CelukRequest.REQUEST_STATUS_ACCEPT:
                vIndicator.setBackgroundResource(R.color.c_green);
                break;
            case CelukRequest.REQUEST_STATUS_REJECT:
                vIndicator.setBackgroundResource(R.color.c_red);
                break;
            default:
                vIndicator.setBackgroundResource(R.color.c_yellow);
                break;
        }
    }
}
