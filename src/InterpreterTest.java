import java.util.ArrayList;

interface MessageContext
{
	String nextToken();

	String skipToken(String skipString);

	String skipReturn();

	boolean hasNextToken();
}

//メッセージの処理を行う
interface MessageProcedure
{
	void process(MessageContext context) throws Exception;
}

//メッセージ全体
class Message implements MessageProcedure
{
	@Override
	public void process(MessageContext context) throws Exception
	{
		// メッセージの識別子
		context.skipToken("MASSAGE_1.0");
		// 改行
		context.skipReturn();
		// メッセージ本体の処理
		new MessageBody().process(context);
		// 空行のチェック
		context.skipReturn();
	}
}

//メッセージ本体
class MessageBody implements MessageProcedure
{

	@Override
	public void process(MessageContext context) throws Exception
	{
		// バージョン
		context.skipToken("EV3LineTracer_1.0");
		// 改行
		context.skipReturn();
		// コマンドの処理
		Command com = new Command();
		com.process(context);
	}

}

//コマンド
class Command implements MessageProcedure
{

	@Override
	public void process(MessageContext context) throws Exception
	{
		// コマンド文字列の取得
		String commandstring = context.nextToken();
		// 改行
		context.skipReturn();

		// コマンドの取得と実行
		createCommand(commandstring).process(context);

	}

	//コマンド文字列に応じたコマンドの生成
	MessageProcedure createCommand(String commandstring) throws Exception
	{
		//SetMDP
		if (commandstring.equals("SetMDP"))
		{
			return new CommandSetMDP();
		}
		//ExecEpisode
		if (commandstring.equals("ExecEpisode"))
		{
			return new CommandExecEpisode();
		}
		//どのコマンドにも当てはまらない場合は例外を投げる
		throw new Exception(this.getClass().getName());
	}
}

//コマンドSetMDP
class CommandSetMDP implements MessageProcedure
{

	@Override
	public void process(MessageContext context) throws Exception
	{
		// TODO コマンド毎の処理を実装する

		// インターバルの読み取り
		new ReadInterval().process(context);
		// 改行
		context.skipReturn();

		// StateCountの読み取り
		ReadStateCount rsc = new ReadStateCount();
		rsc.process(context);
		// StateCountの取得
		int statecount = rsc.getStateCount();
		// 改行
		context.skipReturn();

		// StateIndex毎のcontrolcount格納用
		ArrayList<Integer> controlcount = new ArrayList<Integer>();
		// 読み取ったStateCountの分だけ繰り返すループ
		for (int i = 0; i < statecount; i++)
		{
			ReadState rs = new ReadState(i);
			rs.process(context);
			controlcount.add(rs.getControlCount());
		}
		// TODO Controlの読み取り
		// TODO RegularPolicyの読み取り
	}

}
//コマンドExecEpisode
class CommandExecEpisode implements MessageProcedure
{

	@Override
	public void process(MessageContext context) throws Exception
	{
		// TODO コマンド毎の処理を実装する
	}

}
//Intervalを取得する
class ReadInterval implements MessageProcedure
{
	@Override
	public void process(MessageContext context) throws Exception
	{
		// TODO コマンド毎の処理を実装する
	}

}
//StateCountを取得する
class ReadStateCount implements MessageProcedure
{
	private int StateCount;

	@Override
	public void process(MessageContext context) throws Exception
	{
		//StateCountを取得
		StateCount = Integer.parseInt(context.nextToken());
		//改行
		context.skipReturn();
	}

	public int getStateCount()
	{
		return StateCount;
	}
}

//Stateを取得する
class ReadState implements MessageProcedure
{

	private int StateIndex;
	private int RefMax;
	private int ControlCount;

	ReadState(int stateindex)
	{
		StateIndex = stateindex;
	}

	@Override
	public void process(MessageContext context) throws Exception
	{
		// StateIndexの検証
		if (StateIndex != Integer.parseInt(context.nextToken()))
		{
			throw new Exception(this.getClass().getName());
		}
		// RefMaxの取得
		RefMax = Integer.parseInt(context.nextToken());
		// ControlCountの取得
		ControlCount = Integer.parseInt(context.nextToken());
		// 改行
		context.skipReturn();
	}

	public int getControlCount()
	{
		return ControlCount;
	}

	public int getRefMax()
	{
		return RefMax;
	}

}

//Controlを取得する
class ReadControl implements MessageProcedure
{

	@Override
	public void process(MessageContext context) throws Exception
	{
		// TODO コマンド毎の処理を実装する
	}

}

public class InterpreterTest
{

	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
	}

}
