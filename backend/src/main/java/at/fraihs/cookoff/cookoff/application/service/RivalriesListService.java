package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.shared.web.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.model.Rivalry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RivalriesListService {

    private final CookRivalryRepository cookRivalryRepository;
    private final AccountLookup accountLookup;

    /**
     * No stable DB-level sort is applied - cook names (what a human would sort by) live in
     * the auth module's Account aggregate, not in cook_rivalries, so ordering by name would
     * need a cross-module join. Rows come back in whatever order the query naturally
     * returns (effectively insertion order); revisit if the rivalries list needs a
     * guaranteed display order once it has real usage.
     */
    @Transactional(readOnly = true)
    public PagedResult<Rivalry> execute(int page, int size) {
        Page<Rivalry> rivalries = cookRivalryRepository.findAll(PageRequest.of(page, size))
                .map(this::toGenerated);
        return PagedResult.of(rivalries);
    }

    private Rivalry toGenerated(CookRivalry rivalry) {
        AccountSummary cookA = accountLookup.getById(rivalry.getCookAAccountId());
        AccountSummary cookB = accountLookup.getById(rivalry.getCookBAccountId());
        String headline = RivalryHeadline.build(cookA.name(), cookB.name(),
                rivalry.getCookAWins(), rivalry.getCookBWins(), rivalry.getDraws());
        return new Rivalry(rivalry.getCookAAccountId().toString(), cookA.name(),
                rivalry.getCookBAccountId().toString(), cookB.name(),
                rivalry.getCookAWins(), rivalry.getCookBWins(), rivalry.getDraws(),
                rivalry.getTotalChallenges(), headline);
    }
}
