
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

//改行単位でTokenを読み取る
//行末まで来たらskipReturn()で次の行に進む必要がある。
interface MessageContext
{
	// 次のTokenを取得し現在のTokenを1つ進める
	String nextToken() throws Exception;

	// 次のTokenがskipStringであることを確認し現在のTokenを1つ進める
	void skipToken(String skipString) throws Exception;

	// 次のTokenが行末であることを確認し現在のTokenを次の行の初めに進める
	void skipReturn() throws Exception;

	// 現在の行で次のTokenがあるかどうかを確認する
	boolean hasNextToken();
}

// タブ区切りのメッセージ
class TSVContext implements MessageContext
{
	// メッセージを保持するBufferedReader
	BufferedReader MessageReader;
	// 現在の行をタブ区切りでTokenにわけて配列で保持したもの
	String[] CurrentLine;
	// CurrentLine上での現在位置
	int CurrentIndex;

	// 現在行を次の行に移す
	// 行末に達しているかどうかのチェックは行わない
	private void newLine() throws IOException
	{
		// 次の行を取得
		String newline = MessageReader.readLine();
		// バッファの最後に達したか確認
		if (newline == null)
		{
			throw new IOException("End of Buffer.");
		}
		// 取得した行をタブ区切りに分割
		CurrentLine = newline.split("\t");
		// 現在位置の初期化
		CurrentIndex = -1;
	}

	public TSVContext(BufferedReader br) throws IOException
	{
		MessageReader = br;
		newLine();
	}

	@Override
	public String nextToken() throws Exception
	{
		// 現在位置が行末か確認
		if (!hasNextToken())
		{
			// 行末なら例外発生
			throw new Exception("error on nextToken()");
		}
		// 現在位置を1つ進める
		CurrentIndex++;
		// 進めた位置でのTokenを返却
		return CurrentLine[CurrentIndex];
	}

	@Override
	public void skipToken(String skipString) throws Exception
	{
		// 次のTokenが無ければ例外発生
		if (!hasNextToken())
		{
			throw new Exception("error on skipToken():" + "end of line.");
		}
		// 次のTokenがskipStringと一致して入れば現在位置を進める
		if (CurrentLine[CurrentIndex + 1].equals(skipString))
		{
			CurrentIndex++;
		} else
		{
			// 一致していない場合は例外発生
			throw new Exception(
					"error on skipToken():NExtString is unmatch to "
							+ skipString);
		}

	}

	@Override
	public void skipReturn() throws Exception
	{
		// 現在行に次のTokenがある場合は例外発生
		if (hasNextToken())
		{
			throw new Exception("error on skipReturn():"
					+ "here is not end of line.");
		}
		// 次の行に進む
		newLine();

	}

	@Override
	public boolean hasNextToken()
	{
		// 現在行で次のTokenがあるかを返却する(Tokenがあるならtrue)
		return CurrentIndex != (CurrentLine.length - 1);
	}

}

// メッセージの処理を行う
interface MessageProcedure
{
	void process(MessageContext context) throws Exception;
}

// メッセージ全体
// MESSAGE_1.0 ;メッセージ開始
// <MessageBody> ;メッセージの本体
// ;(空行)メッセージ終了
class Message implements MessageProcedure
{
	@Override
	public void process(MessageContext context) throws Exception
	{
		// メッセージの識別子
		context.skipToken("MESSAGE_1.0");
		// 改行
		context.skipReturn();
		// メッセージ本体の処理
		new MessageBody().process(context);
		// 空行のチェック
		context.skipToken("");
	}
}

// メッセージ本体
// <MessageBody>:
// <Version> ;バージョン
// <Command>|<Result> ;コマンド|コマンドの結果
// /////////////////////////////////////////////
// <Result>は現状実装予定なし(Output専用)
// EV3はコマンドを受け付けて結果を出力するのみの予定
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

// コマンド
// <Command>:
// <CommandCategory>;コマンド種別
// <Body> ;コマンドの内容
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

	// コマンド文字列に応じたコマンドの生成
	MessageProcedure createCommand(String commandstring) throws Exception
	{
		// SetMDP
		if (commandstring.equals("SetMDP"))
		{
			return new CommandSetMDP();
		}
		// ExecEpisode
		if (commandstring.equals("ExecEpisode"))
		{
			return new CommandExecEpisode();
		}
		// どのコマンドにも当てはまらない場合は例外を投げる
		throw new Exception(this.getClass().getName());
	}
}

// コマンドSetMDP
// <int> ;Interval(msec)
// <int> ;State数
// <int> <double> <int> ;Stateの定義： StateIndex RefMax ControlCount
// (上の行をStateCount回繰り返し)
// <int> <int> <int> <int> ;Controlの定義： StateIndex ControlIndex LMotorSpeed
// RMotorSpeed
// (上の行をStateCount*ControlIndex回繰り返し)
// <int> <int> ;RegularPolicyの定義： StateIndex ControlIndex
// (上の行をStateCount回繰り返し)
class CommandSetMDP implements MessageProcedure
{

	@Override
	public void process(MessageContext context) throws Exception
	{

		// インターバルの読み取り
		new ReadInterval().process(context);

		// StateCountの読み取り
		ReadStateCount rsc = new ReadStateCount();
		rsc.process(context);
		// StateCountの取得
		int statecount = rsc.getStateCount();

		// StateIndex毎のcontrolcount格納用
		ArrayList<Integer> controlcount = new ArrayList<Integer>();

		// State読み取り用MessageProcedure
		ReadState rs = new ReadState();
		// 読み取ったStateCountの分だけ繰り返すループ
		for (int i = 0; i < statecount; i++)
		{
			// rsに読み取るStateIndexを設定
			rs.setStateIndex(i);
			// Stateの読み取り
			rs.process(context);
			// 取得したControlCountを保持
			controlcount.add(rs.getControlCount());
		}
		// Control読み取り用MessageProcedure
		ReadControl rc = new ReadControl();
		for (int i = 0; i < statecount; i++)
		{
			// rsに読み取るStateIndexを設定
			rc.setStateIndex(i);
			for (int j = 0; j < controlcount.get(i); j++)
			{
				// rsに読み取るControlIndexを設定
				rc.setControlIndex(j);
				// Controlの読み取り
				rc.process(context);
			}
		}
		// RegularPolicyの読み取り
		ReadRegularPolicy rrp = new ReadRegularPolicy();
		rrp.setStateCount(statecount);
		rrp.setControlCount(controlcount);
		rrp.process(context);
	}
}

// コマンドExecEpisode
// Body部は無し
class CommandExecEpisode implements MessageProcedure
{

	@Override
	public void process(MessageContext context) throws Exception
	{
		// TODO コマンド毎の処理を実装する
	}

}

// Intervalを取得する
class ReadInterval implements MessageProcedure
{
	private int Interval;

	public int getInterval()
	{
		return Interval;
	}

	@Override
	public void process(MessageContext context) throws Exception
	{
		// Intervalを取得
		Interval = Integer.parseInt(context.nextToken());
		// 改行
		context.skipReturn();
	}

}

// StateCountを取得する
class ReadStateCount implements MessageProcedure
{
	private int StateCount;

	@Override
	public void process(MessageContext context) throws Exception
	{
		// StateCountを取得
		StateCount = Integer.parseInt(context.nextToken());
		// 改行
		context.skipReturn();
	}

	public int getStateCount()
	{
		return StateCount;
	}
}

// Stateを取得する
class ReadState implements MessageProcedure
{

	private int StateIndex;
	private double RefMax;
	private int ControlCount;

	public void setStateIndex(int stateindex)
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
		RefMax = Double.parseDouble(context.nextToken());
		// ControlCountの取得
		ControlCount = Integer.parseInt(context.nextToken());
		// 改行
		context.skipReturn();
	}

	public int getControlCount()
	{
		return ControlCount;
	}

	public double getRefMax()
	{
		return RefMax;
	}

}

// Controlを取得する
class ReadControl implements MessageProcedure
{
	int StateIndex;
	int ControlIndex;
	int LMotorSpeed;
	int RMotorSpeed;

	public void setStateIndex(int i)
	{
		StateIndex = i;
	}

	public void setControlIndex(int j)
	{
		ControlIndex = j;

	}

	@Override
	public void process(MessageContext context) throws Exception
	{
		// StateIndexの検証
		if (StateIndex != Integer.parseInt(context.nextToken()))
		{
			throw new Exception(this.getClass().getName());
		}
		// ControlIndexの検証
		if (ControlIndex != Integer.parseInt(context.nextToken()))
		{
			throw new Exception(this.getClass().getName());
		}
		// LMotorSpeedの取得
		LMotorSpeed = Integer.parseInt(context.nextToken());
		// LMotorSpeedの取得
		RMotorSpeed = Integer.parseInt(context.nextToken());
		// 改行
		context.skipReturn();
	}

}

class ReadRegularPolicy implements MessageProcedure
{
	private int StateCount;
	private ArrayList<Integer> ControlCount;


	/**
	 * @param stateCount the stateCount to set
	 */
	public void setStateCount(int stateCount)
	{
		StateCount = stateCount;
	}

	/**
	 * @param controlCount the controlCount to set
	 */
	public void setControlCount(ArrayList<Integer> controlCount)
	{
		ControlCount = controlCount;
	}

	
	@Override
	public void process(MessageContext context) throws Exception
	{
		for (int i = 0; i < StateCount; i++)
		{
			if (i != Integer.parseInt(context.nextToken()))
			{
				throw new Exception(this.getClass().getName());
			}
			int j=Integer.parseInt(context.nextToken());
			if(j<0 || ControlCount.get(i)<=j)
			{
				throw new Exception(this.getClass().getName());
			}
			// (i,j)をRegularPolicyとしてセットする
			context.skipReturn();
		}
	}



}

public class InterpreterTest
{

	public static void main(String[] args)
	{
		String set_mdp_body = 
				"MESSAGE_1.0\n" 
				+"EV3LineTracer_1.0\n"
				+"SetMDP\n"
				+"11"+"\n"
				+"10"+"\n"
				+"0	0.1	1"+"\n"
				+"1	0.2	2"+"\n"
				+"2	0.3	2"+"\n"
				+"3	0.4	2"+"\n"
				+"4	0.5	2"+"\n"
				+"5	0.6	2"+"\n"
				+"6	0.7	2"+"\n"
				+"7	0.8	2"+"\n"
				+"8	0.9	2"+"\n"
				+"9	1.0	2"+"\n"
				+"0	0	10	10"+"\n"
				+"1	0	10	5"+"\n"
				+"1	1	5	10"+"\n"
				+"2	0	10	5"+"\n"
				+"2	1	5	10"+"\n"
				+"3	0	10	5"+"\n"
				+"3	1	5	10"+"\n"
				+"4	0	10	5"+"\n"
				+"4	1	5	10"+"\n"
				+"5	0	10	5"+"\n"
				+"5	1	5	10"+"\n"
				+"6	0	10	5"+"\n"
				+"6	1	5	10"+"\n"
				+"7	0	10	5"+"\n"
				+"7	1	5	10"+"\n"
				+"8	0	10	5"+"\n"
				+"8	1	5	10"+"\n"
				+"9	0	10	5"+"\n"
				+"9	1	5	10"+"\n"
				+"0	0"+"\n"
				+"1	1"+"\n"
				+"2	1"+"\n"
				+"3	0"+"\n"
				+"4	0"+"\n"
				+"5	1"+"\n"
				+"6	1"+"\n"
				+"7	0"+"\n"
				+"8	1"+"\n"
				+"9	1"+"\n"
				+"\n";
		try
		{
			BufferedReader br = new BufferedReader(new StringReader(
					set_mdp_body));
			TSVContext tsvc = new TSVContext(br);
			new Message().process(tsvc);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
