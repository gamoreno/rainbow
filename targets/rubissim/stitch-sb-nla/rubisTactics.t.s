module rubis.tactics;

import model "RubisSys:Acme" { RubisSys as M, RubisFam as T};
import op "org.sa.rainbow.stitch.lib.*";
import op "org.sa.rainbow.model.acme.rubis.Rubis";

define int numberOfServers = Set.size({ select s : T.ServerT in M.components | true });

tactic TIncDimmer() {
    condition {
	RubisUtils.dimmerFactorToLevel(M.LB0.dimmer, M.DIMMER_LEVELS, M.DIMMER_MARGIN) < M.DIMMER_LEVELS;
    }
    action {
        M.setDimmer(M.LB0, RubisUtils.dimmerLevelToFactor(RubisUtils.dimmerFactorToLevel(M.LB0.dimmer, M.DIMMER_LEVELS, M.DIMMER_MARGIN) + 1, M.DIMMER_LEVELS, M.DIMMER_MARGIN));
    }
    effect {
    }
}

tactic TDecDimmer() {
    condition {
	RubisUtils.dimmerFactorToLevel(M.LB0.dimmer, M.DIMMER_LEVELS, M.DIMMER_MARGIN) > 1;
    }
    action {
        M.setDimmer(M.LB0, RubisUtils.dimmerLevelToFactor(RubisUtils.dimmerFactorToLevel(M.LB0.dimmer, M.DIMMER_LEVELS, M.DIMMER_MARGIN) - 1, M.DIMMER_LEVELS, M.DIMMER_MARGIN));
    }
    effect {
    }
}

tactic TSetMinDimmer() {
    condition {
	true;
    }
    action {
        M.setDimmer(M.LB0, M.DIMMER_MARGIN);
    }
    effect {
    }
}


tactic TSetMaxDimmer() {
    condition {
	true;
    }
    action {
        M.setDimmer(M.LB0, 1 - M.DIMMER_MARGIN);
    }
    effect {
    }
}

tactic TAddServer() {
    condition {
	Rubis.availableServices(M, T.ServerT) > 0;
    }
    action {
	// add first server not enabled
	// note that the associated effector adds the next server in the
	// secuence, regardless of the one selected here
	set servers = Rubis.findServices(M, T.ServerT);
	object newServer = RubisUtils.minOverProperty("index", servers);
	M.addServer(M.LB0, newServer);	
    }
    effect {
	false; // force it to wait for the timeout value in the strategy
    }
}

tactic TRemoveServer() {
    condition {
	numberOfServers > 1;
    }
    action {
	// remove the server with the highest index
	// note that the associated effector removes the server with the highest index
	// regardless of the one selected here
	set servers = { select s : T.ServerT in M.components | true };
	object lastServer = RubisUtils.maxOverProperty("index", servers);
	M.removeServer(M.LB0, lastServer);	
    }
    effect {
    }
}

