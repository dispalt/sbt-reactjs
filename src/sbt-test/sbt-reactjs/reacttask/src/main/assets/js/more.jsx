var MoreMessage = React.createClass({
  render () {
    var names: Array<string> = this.props.names;
    return <div>{names.map((name) => <span>{name}</span>)}</div>;
  }
});

React.render(<MoreMessage names={["John", "Mary"]} />, mountNode);
