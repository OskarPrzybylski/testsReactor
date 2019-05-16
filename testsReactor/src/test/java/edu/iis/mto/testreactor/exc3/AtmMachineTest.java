package edu.iis.mto.testreactor.exc3;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import org.junit.Assert;
import org.junit.Test;

public class AtmMachineTest {

    private BankService bankService;
    private CardProviderService cardProviderService;
    private MoneyDepot moneyDepot;
    private AtmMachine atmMachine;
    private Money money;
    private Card card;
    private AuthenticationToken token;

    public AtmMachineTest()
    {
        bankService = mock(BankService.class);
        cardProviderService = mock(CardProviderService.class);
        moneyDepot = mock(MoneyDepot.class);
        money = Money.builder().withAmount(100).withCurrency(Currency.PL).build();
        card = Card.builder().withCardNumber("string").withPinNumber(100).build();
        token = AuthenticationToken.builder().withAuthorizationCode(111).withUserId("111").build();
    }

    private void SetMoneyDepotFalse(){
        when(moneyDepot.releaseBanknotes(anyList())).thenReturn(false);
    }

    private void SetMoneyDepotTrue(){
        when(moneyDepot.releaseBanknotes(anyList())).thenReturn(true);
    }

    private void SetCardProviderServiceReturningToken(){
        when(cardProviderService.authorize(any())).thenReturn(java.util.Optional.of(token));
    }

    private void SetCardProviderServiceNotReturningToken(){
        when(cardProviderService.authorize(any())).thenReturn(java.util.Optional.empty());
    }

    private void SetBankServiceFalse(){
        when(bankService.charge(token, money)).thenReturn(false);
    }

    private void SetBankServiceTrue(){
        when(bankService.charge(token, money)).thenReturn(true);
    }
    @Test
    public void itCompiles()
    {
        assertThat(true, equalTo(true));
    }

    @Test(expected = MoneyDepotException.class)
    public void Check_AtmMachine_Withdraw_method_Is_Throwing_MoneyDepotException_When_MoneyDepot_Not_Working()
    {
        //arrange
        SetMoneyDepotFalse();
        SetCardProviderServiceReturningToken();
        SetBankServiceTrue();
        //act
        atmMachine = new AtmMachine(cardProviderService,bankService,moneyDepot);
        atmMachine.withdraw(money, card);
        //assert is exception
    }

    @Test(expected = InsufficientFundsException.class)
    public void Check_AtmMachine_Withdraw_method_Is_Throwing_InSufficientFoundsException_When_BankService_Charge_Method_Is_returning_false()
    {
        //arrange
        SetMoneyDepotTrue();
        SetCardProviderServiceReturningToken();
        SetBankServiceFalse();
        //act
        atmMachine = new AtmMachine(cardProviderService,bankService,moneyDepot);
        atmMachine.withdraw(money, card);
        //assert is exception
    }

    @Test(expected = CardAuthorizationException.class)
    public void Check_AtmMachine_Withdraw_method_Is_Throwing_CardAuthorizationException_When_Card_Cannot_Be_Authorized()
    {
        //arrange
        SetMoneyDepotTrue();
        SetCardProviderServiceNotReturningToken();
        SetBankServiceTrue();
        //act
        atmMachine = new AtmMachine(cardProviderService,bankService,moneyDepot);
        atmMachine.withdraw(money, card);
        //assert is exception
    }

    @Test
    public void Check_AtmMachine_Withdraw_method_Is_Returning_Payment_With_100PL_When_Money_Was_100PL(){
        //arrange
        SetBankServiceTrue();
        SetCardProviderServiceReturningToken();
        SetMoneyDepotTrue();
        //act
        atmMachine = new AtmMachine(cardProviderService,bankService,moneyDepot);
        Payment payment = atmMachine.withdraw(money, card);
        //assert
        int sum = 0;
        for(Banknote item : payment.getValue()){
            sum += item.getValue();
        }
        Assert.assertTrue(sum == money.getAmount());
    }

    @Test(expected = WrongMoneyAmountException.class)
    public void Check_AtmMachine_Withdraw_method_Is_Throwing_WorngMoneyAmountException_When_Money_Is_Negative(){
        //arrange
        SetMoneyDepotTrue();
        SetBankServiceTrue();
        SetCardProviderServiceReturningToken();
        Money negativeMoney = Money.builder().withCurrency(Currency.PL).withAmount(-100).build();
        //act
        atmMachine = new AtmMachine(cardProviderService,bankService,moneyDepot);
        atmMachine.withdraw(negativeMoney, card);
        //assert is exception
    }

    @Test(expected = NullPointerException.class)
    public void Check_AtmMachine_Cannot_Build_When_BankService_Is_Null(){
        //arrange
        SetMoneyDepotTrue();
        SetBankServiceTrue();
        SetCardProviderServiceReturningToken();
        //act
        atmMachine = new AtmMachine(cardProviderService,null,moneyDepot);
        //assert is exception
    }

    @Test
    public void Check_AtmMachine_Will_Use_CardProviderService_Authorize_Method_Only_Once_Per_Transaction(){
        //arrange
        SetMoneyDepotTrue();
        SetBankServiceTrue();
        SetCardProviderServiceReturningToken();
        //act
        atmMachine = new AtmMachine(cardProviderService,bankService,moneyDepot);
        atmMachine.withdraw(money, card);
        //assert
        verify(cardProviderService, times(1)).authorize(card);
    }

    @Test
    public void Check_AtmMachine_Will_Abort_Bank_Operation_If_BankService_Cannot_Charge_Money(){
        //arrange
        SetMoneyDepotTrue();
        SetBankServiceFalse();
        SetCardProviderServiceReturningToken();
        //act
        try{
            atmMachine = new AtmMachine(cardProviderService,bankService,moneyDepot);
            atmMachine.withdraw(money, card);
        }
        catch(Exception e){

        }
        //assert
        verify(bankService, times(1)).abort(token);
    }

    @Test
    public void Check_AtmMachine_Will_Abort_Bank_Operation_If_MoneyDepot_Cannot_Withdraw_Money(){
        //arrange
        SetMoneyDepotFalse();
        SetBankServiceTrue();
        SetCardProviderServiceReturningToken();
        //act
        try{
            atmMachine = new AtmMachine(cardProviderService,bankService,moneyDepot);
            atmMachine.withdraw(money, card);
        }
        catch(Exception e){

        }
        //assert
        verify(bankService, times(1)).abort(token);
    }

}
