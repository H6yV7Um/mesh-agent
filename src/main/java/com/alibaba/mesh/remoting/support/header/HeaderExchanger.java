package com.alibaba.mesh.remoting.support.header;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.Transporters;
import com.alibaba.mesh.remoting.exchange.ExchangeClient;
import com.alibaba.mesh.remoting.exchange.ExchangeHandler;
import com.alibaba.mesh.remoting.exchange.ExchangeServer;
import com.alibaba.mesh.remoting.exchange.Exchanger;
import com.alibaba.mesh.remoting.transport.DecodeHandlerSupport;

/**
 * DefaultMessenger
 *
 *
 */
public class HeaderExchanger implements Exchanger {

    public static final String NAME = "header";

    @Override
    public ExchangeClient connect(URL url, ExchangeHandler handler) throws RemotingException {
         return new HeaderExchangeClient(Transporters.connect(url, new DecodeHandlerSupport(new HeaderExchangeHandler(handler))), true);
    }

    @Override
    public ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
        return new HeaderExchangeServer(Transporters.bind(url, new DecodeHandlerSupport(new HeaderExchangeHandler(handler))));
    }

}
